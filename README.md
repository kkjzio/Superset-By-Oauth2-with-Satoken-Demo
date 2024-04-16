## superset和Oauth2服务器登录的大致交互流程

![](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/supset登录.jpg)



## demo中软件的规格

+ java 8
+ superset(docker版) 3.11
+ redis 5.0
+ mysql 5.7



## 环境准备

### 1.配置安装superset

1. 安装superset的docker版，如果安装本地或者docker-compose可以参照官网：https://superset.apache.org/docs/installation/installing-superset-using-docker-compose

```shell
docker pull apache/superset:3.1.1
```



2.准备配置文件

```shell
# 创建配置文件挂载目录
mkdir ~/pythonpath

# 将本项目目录下的pythonpath的文件放入新建的pythonpath目录下
cp -r ./pythonpath ~/pythonpath
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
docker run -d -p 8088:8088 \
             -e "SUPERSET_SECRET_KEY=uQU7ZnWbPeSAtsBA3WXXasEPdL3fZSYb" \
             -e "TALISMAN_ENABLED=False" \
             -v ~/pythonpath:/app/pythonpath \
             --name superset apache/superset:3.1.1 

# 初始化数据库
docker exec -it superset superset db upgrade
# 下载示例，但下载源是github，网络很容易不通，建议不做此操作
# docker exec -it superset superset load_examples
# 初始化superset
docker exec -it superset superset init

# 建立admin超级管理员用户，用于管理以及后端查询用
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





### 2.配置javaSpring项目

这里demo是在sa-token官方的项目上修改而来，源地址：

```
https://github.com/dromara/Sa-Token/tree/dev/sa-token-demo/sa-token-demo-oauth2
```



1. 打开`sa-token-demo-oauth2/sa-token-demo-oauth2-server/src/main/resources/application.yml`，修改redis、superset中的配置信息，**这里的superset.user中的用户应有较高的查询权限，后端会用此账户查询面板和筛选器信息**。



2. 导入打开项目下的`sql`文件夹，导入`bilibili.sql`，数据库名为`bilibili`



3. 依照satoken官方的要求，更改hosts:

   ```
   首先在host文件 (C:\windows\system32\drivers\etc\hosts) 添加以下内容: 
   	127.0.0.1 sa-oauth-server.com 
   	127.0.0.1 sa-oauth-client.com 
   再从浏览器访问：
   	http://sa-oauth-client.com:8002
   ```



4. 分别启动client和server两个项目，redis也不要忘记



## 使用方法

### 1. 设置superset

首先浏览器打开

```
http://127.0.0.1:8088
```

进入superset页面

![image-20240416112303068](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416112303068.png)

点击登录后，使用最高权限的管理员登录

```shell
# 管理员
username:sa
password:123456
```



通过服务器认证后进入主界面，设置连接数据库和建立图表，demo中主要是用了`bilibili`数据库中的`video`表，主要要导入`videoID`、`videocAtegory`两个字段，下列为生成的图表字段参考

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
http://sa-oauth-client.com:8002
```

点击授权码登录中的显示登录

![image-20240416111738547](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416111738547.png)

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

![image-20240416114113907](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416114113907.png)

![image-20240416114209786](https://raw.githubusercontent.com/kkjzio/Superset-By-Oauth2-with-Satoken-Demo/main/README.assets/image-20240416114209786.png)





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







