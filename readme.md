## 1 部署Fisco-bcos区块链

### 1.1. 安装区块链依赖

Fisco-bcos`开发部署工具 build_chain.sh`脚本依赖于`openssl, curl`，根据您使用的操作系统，使用以下命令安装依赖。

##### 安装macOS依赖

```
# 最新homebrew默认下载的为openssl@3，需要指定版本openssl@1.1下载
brew install openssl@1.1 curl

openssl version
OpenSSL 1.1.1n  15 Mar 2022
```

##### 安装ubuntu依赖

```
sudo apt install -y openssl curl
```

##### 安装centos依赖

```
sudo yum install -y openssl openssl-deve
```

注：后续示例环境为：Ubuntu 22.04

### 1.2部署区块链节点

```
## 创建操作目录
cd ~ && mkdir -p fisco && cd fisco

## 下载脚本
curl -#LO https://github.com/FISCO-BCOS/FISCO-BCOS/releases/download/v2.11.0/build_chain.sh && chmod u+x build_chain.sh


## 搭建国密版本4节点区块链，请确保机器的30300~30303，20200~20203，8545~8548端口没有被占用
cd ~/fisco
bash build_chain.sh -l 127.0.0.1:4 -p 30300,20200,8545 -g -G
```

启动区块链节点，在后续操作中需要保持区块链节点处于启动状态

```
## 启动区块链节点
bash ~/fisco/nodes/127.0.0.1/start_all.sh
```



## 2 配置SM2_CPPA后端服务

后端服务JAVA版本为：1.8

### 2.1导入配置文件

将/fisco/nodes/127.0.0.1/sdk下的文件复制到/SM2_CPPA/src/main/resources/conf中

```
#导入节点配置文件，path为实际的父路径
cp -r  ~/fisco/nodes/127.0.0.1/sdk/*  path/BCPPA/src/main/resources/conf/
```



## 3配置SM2_CPPA安卓端

找到app\build\outputs\apk\debug下的aap-debug.apk文件，传输到安卓手机中进行安装。共需要安装3个app。
前两个安装SM2_CPPA，安装前进入app\src\main\java\com\example\SM2_CPPA下的MainActivity中，改动其中的address，使得两个app的address不一样。
第三个直接安装CA端的app即可。




## 4测试函数接口功能

### 4.1 启动后端服务

在IDEA中打开SM2CPPA项目文件夹并启动运行。

### 4.2 启动安卓端

先将两个安装SM2_CPPA的手机连到同一局域网中，一台手机先选择一个端口开启服务，另一台选择连接服务，并输入对方的IP地址和端口号。
CA可在任意一台手机上启动。

### 4.3测试函数接口功能

用户端（address2）先点击“初始化用户”，CA端再进行“部署合约”、“注册公钥”、“公钥派生”和“批量注册公钥”。用户端（address2）此时可以点击“获取TxID”，进行消息的发送。
另一用户（address1）可以点击“发送TxID获取公钥”来获取公钥。并点击“验证签名”来验证。
此后，用户（address2）可以点击“私钥派生”并再次“获取TxID”来发送下一次的消息。
CA可以点击“撤销公钥”来撤销公钥，使得用户无法继续使用该公钥。（这里默认注销的是点击“公钥派生”派生出的那个公钥）



