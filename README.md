## superset和Oauth2服务器登录的大致交互流程

![](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/supset登录.jpg)



## demo中软件的规格

+ java 8
+ superset(docker版) 3.11
+ redis 5.0
+ mysql 5.7



## 环境准备

### 1.配置hosts和nginx

因为这是本地测试，需要使用hosts指定域名ip，并且由于为了form框架跨源显示supertset的报表，因此需要配置nginx

以下是hosts的参考配置：

```
127.0.0.1 mydomain.com
127.0.0.1 superset.mydomain.com
127.0.0.1 server.mydomain.com
```



以下是nginx的参考配置

```nginx
http {
    include       mime.types;
    default_type  application/octet-stream;

    #log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
    #                  '$status $body_bytes_sent "$http_referer" '
    #                  '"$http_user_agent" "$http_x_forwarded_for"';

    #access_log  logs/access.log  main;

    sendfile        on;
    #tcp_nopush     on;

    #keepalive_timeout  0;
    keepalive_timeout  65;

    #gzip  on;

    server {
        listen 80;
        server_name mydomain.com;

        location / {
            proxy_pass http://127.0.0.1:8002/; # 这里是你的第一个服务器地址
            proxy_redirect  off; # 可以使得浏览器地址栏不会发生变化，即不会跳转到代理服务器地址
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

            # 添加以下头信息以解决跨域问题
            if ($http_origin ~* (\.mydomain\.com$)) {
                set $allow_origin $http_origin;
            }
            add_header Access-Control-Allow-Origin $allow_origin;
            # add_header Access-Control-Allow-Origin *;
            add_header Access-Control-Allow-Headers X-Requested-With;
            add_header Access-Control-Allow-Methods GET,POST,OPTIONS;
        }
            
    }

    server {
    listen 80;
    server_name server.mydomain.com;

    location / {
        proxy_pass http://127.0.0.1:8001; # 这里是你的第服务器地址
            proxy_redirect  off; # 可以使得浏览器地址栏不会发生变化，即不会因302跳转到代理服务器地址
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

            # 添加以下头信息以解决跨域问题
            add_header Access-Control-Allow-Origin *;
            add_header Access-Control-Allow-Headers X-Requested-With;
            add_header Access-Control-Allow-Methods GET,POST,OPTIONS;
    }

    }

    server {
    listen 80;
    server_name superset.mydomain.com;

    location / {
        proxy_pass http://192.168.59.129:8088/; 
            proxy_redirect  off; # 可以使得浏览器地址栏不会发生变化，即不会因302跳转到代理服务器地址
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

            # 添加以下头信息以解决跨域问题
            # 如果更改域名也记得更改这里的正则匹配公式
            if ($http_origin ~* ^http(s)?\:\/\/(superset\.)?mydomain\.com$) {
                set $allow_origin $http_origin;
            }
            add_header Access-Control-Allow-Origin $allow_origin;
            # add_header Access-Control-Allow-Origin *;
            add_header Access-Control-Allow-Headers "X-Requested-With, Content-Type";
            add_header Access-Control-Allow-Methods GET,POST,OPTIONS;
            add_header Access-Control-Allow-Credentials true; # 允许跨域凭证
    }
    }


  
}
```





### 2. 配置安装superset

1. 安装superset的docker版，如果安装本地或者docker-compose可以参照官网：https://superset.apache.org/docs/installation/installing-superset-using-docker-compose

```shell
docker pull apache/superset:3.1.1
```



2.准备配置文件

```shell
# 创建配置文件挂载目录
mkdir ~/pythonpath
```



3.更改`superset_confing.py`中的配置，以下为参考配置

```python
# 启动数据源
SQLALCHEMY_DATABASE_URI = "mysql://root:123456@192.168.59.129:3306/superset?charset=utf8"
....

# oauth2的服务配置
OAUTH_PROVIDERS = [
    {   'name':'egaSSO',
        'token_key':'access_token', # Name of the token in the response of access_token_url
        'icon':'fa-address-card',   # Icon for the provider
        'remote_app': {
            'client_id':'1001',  # Client Id (Identify Superset application)
            'client_secret':'aaaa-bbbb-cccc-dddd-eeee', # Secret for this Client Id (Identify Superset application)
            'access_token_method':'GET',
            'client_kwargs':{
                'scope': 'userinfo', # 传给后端认证域
            },
            'access_token_params':{        # Additional parameters for calls to access_token_url
                'client_id':'1001',
                'client_secret':'aaaa-bbbb-cccc-dddd-eeee'
            },
            'access_token_method':'POST',    # HTTP Method to call access_token_url
            'api_base_url':'http://192.168.59.1:8001/oauth2/',
            'access_token_url':'http://192.168.59.1:8001/oauth2/token',
            'authorize_url':'http://192.168.59.1:8001/oauth2/authorize'
        }
    }
]
```

字段具体说明可以参考官方文档中的，如有需求可以自行更改:

```
https://superset.apache.org/docs/installation/configuring-superset#custom-oauth2-configuration
```



4. 运行并初始化数据库

```shell
# 启动 并配置映射路径和SUPERSET_SECRET_KEY
# 使用前记得自己修改对应路径和地址
docker run -d -p 8088:8088 \
             -e "SUPERSET_SECRET_KEY=uQU7ZnWbPeSAtsBA3WXXasEPdL3fZSYb" \
             -e "TALISMAN_ENABLED=False" \
             --add-host=server.mydomain.com:192.168.59.1 \
             -v ~/pythonpath:/app/pythonpath \
             --name superset apache/superset:3.1.1 

# 安装authlib
docker exec -it superset pip install authlib==1.3.0

# 将本项目目录下的pythonpath的文件放入新建的pythonpath目录下
cp -r ./pythonpath ~/pythonpath

# 重启容器
docker restart superset

# 这里可以用项目sql中提供的sql文件，将数据载入到sql中

# 初始化数据库
# docker exec -it superset superset db upgrade
# 下载示例，但下载源是github，网络很容易不通，建议不做此操作
# docker exec -it superset superset load_examples
# 初始化superset
# docker exec -it superset superset init

# 建立admin超级管理员用户，用于管理以及后端查询用
# 如果读取了sql文件就不用创建了
docker exec -it superset superset fab create-admin \
              --username admin \
              --firstname Admin \
              --lastname Admin \
              --email admin@localhost \
              --password admin

# 删除
# docker stop superset
# docker container rm -f superset
```





### 3. 配置javaSpring项目

这里demo是在sa-token官方的项目上修改而来，源地址：

```
https://github.com/dromara/Sa-Token/tree/dev/sa-token-demo/sa-token-demo-oauth2
```



1. 打开`sa-token-demo-oauth2/sa-token-demo-oauth2-server/src/main/resources/application.yml`，修改redis、superset中的配置信息，**这里的superset.user中的用户应有较高的查询权限，后端会用此账户查询面板和筛选器信息**。



2. 导入打开项目下的`sql`文件夹，导入`bilibili.sql`，数据库名为`bilibili`



3. 按顺序启动：

+ nginx
+ redis
+ server 和 client



## 使用方法

### 1. 设置superset（这步可以通过读取项目的`sql/superset.sql`文件跳过）

首先浏览器打开

```
http://superset.mydomain.com/
```

进入superset页面

![image-20240416112303068](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416112303068.png)

点击登录后，使用最高权限的管理员登录

```shell
# 管理员
username:sa
password:123456
```



通过服务器认证后进入主界面，设置连接数据库和建立图表，demo中主要是用了`bilibili`数据库中的`video`表(该测试数据在目录下的`sql/bilibili.sql`)，主要要导入`videoID`、`videocAtegory`两个字段，下列为生成的图表字段参考

![image-20240416112923877](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416112923877.png)



之后在编辑仪表盘中，加上slug，这个slug会被作为前端指定的的仪表盘对象，所以做表之前要确认好，这里用了demo中展示用的slug`Test-Dashboard`

![image-20240416113031891](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416113031891.png)



接着准备两个筛选器:

![image-20240416113319170](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416113319170.png)



两个筛选器的设置值如下：

`VideoType`:

![image-20240416113528544](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416113528544.png)

`videoID`:

![image-20240416113633915](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416113633915.png)



设置完毕后，不要忘记点击发布，点击发布后的状态应该为如下：

![image-20240416113737552](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416113737552.png)



### 2.使用satoken的oauth2服务跳转superset

```
http://mydomain.com/
```

点击授权码登录中的显示登录

![image-20240508155539780](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240508155539780.png)

这里设置了两种用户权限，管理员（~~这步其实可以跳过，但是我没改demo的认证逻辑~~）

```shell
# 管理员
username:sa
password:123456
# 普通用户
user:user
password:123456
```



登录后使用最下面的选项框即可动态使用筛选器

![image-20240508142934332](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240508142934332.png)

![image-20240508143014089](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240508143014089.png)



### 3.动态参数使用SurpsetNativeFillter显示图表

我在另外一篇博客中详细介绍了如何使用筛选器，及自定义筛选参数

> https://www.kkjz.xyz/index.php/archives/240/



### 4.通过api，使用sqlLab生成临时表格，并生成xlxs下载链接

sqllab可以通过自定义sql语句，动态的控制表格内容，具体使用自定义方法可以参考我的这个博客

> https://www.kkjz.xyz/index.php/archives/241/

+ 输入视频id的最小值、最大值，异步加载界面，并且可以下载xlxs文件

![image-20240508143301920](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240508143301920.png)



## 二次开发的一些注意事项

+ superset的api文档在

  ```
  http://127.0.0.1:8088/swagger/v1
  ```

+ `pythonpath/custom_sso_security_manager.py`文件中有这样一段

  ```python
  ....
  return { 'name' : m2['nickname'], 'email' : m2['email'], 'username' : m2['username'], 'first_name': m2['first_name'], 'last_name': m2['last_name']}
  ```

  其中的`'username' : m2['username']`这个字段会成为superset判断登录用户的判断依据



## 一些注意事项

+ https://blog.csdn.net/baijiafan/article/details/126501682

CORS常见的错误，post出错，get不出错
大量现代浏览器似乎会在post之前传输一个options来确认服务器行为，该现象在ios和android之间不统一。

如果你的web服务器没有能够处理该行为的关联关系，可能会出现get接口都是好的，但是post出错的情况，这时，我们需要为options操作配置一个特殊的返回结构，例如我们用到的（从网上摘抄）

```
if ($request_method = 'OPTIONS') {
        add_header 'Access-Control-Allow-Origin' '*';
        #
        # Om nom nom cookies
        #
        add_header 'Access-Control-Allow-Credentials' 'true';
        add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS';
        #
        # Custom headers and headers various browsers *should* be OK with but aren't
        #
        add_header 'Access-Control-Allow-Headers' 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type';
        #
        # Tell client that this pre-flight info is valid for 20 days
        #
        add_header 'Access-Control-Max-Age' 1728000;
        add_header 'Content-Type' 'text/plain charset=UTF-8';
        add_header 'Content-Length' 0;
        return 204;
     }
```

Options因为只是预请求，所以我们就不用麻烦去配 Access-Control-Allow-Origin了，反正正式请求的时候再判断也可以。



+ 当你设置了 `Access-Control-Allow-Credentials: true` 时，你不能将 `Access-Control-Allow-Origin` 设置为 `*`。你需要将 `Access-Control-Allow-Origin` 设置为具体的源 URL。