

```shell
docker pull apache/superset:3.1.1
```



准备启动

```shell
# 创建配置文件挂载目录
mkdir /home/kkjz/superset/pythonpath
# 启动
docker run -d -p 8088:8088 \
             -e "SUPERSET_SECRET_KEY=uQU7ZnWbPeSAtsBA3WXXasEPdL3fZSYb" \
             -e "TALISMAN_ENABLED=False" \
             -v /home/kkjz/superset/pythonpath:/app/pythonpath \
             --name superset apache/superset:3.1.1 

# 删除
docker stop superset
docker container rm -f superset
```



```shell
# 加载数据库
docker exec -it superset superset db upgrade
# 下载示例，但下载源是github，网络很容易不通，建议不做此操作
# docker exec -it superset superset load_examples
# 初始化superset
docker exec -it superset superset init

# superset db migrate
# superset db update
```





set

```shell
docker exec -it superset superset fab create-admin \
              --username admin \
              --firstname Admin \
              --lastname Admin \
              --email admin@localhost \
              --password admin
              
superset fab create-admin --username admin --firstname Admin --lastname Admin --email admin@localhost --password admin
```





寻找pemmison_name

`class_permission_name`





## LocalProxy代理对象

https://www.cnblogs.com/fengff/p/12510738.html



http://192.168.59.129:8088/superset/dashboard/p/1rMPZNMgJLm/

```bash
curl -X 'GET' 'http://192.168.59.129:8088/api/v1/dashboard/' -H 'accept: application/json'
```