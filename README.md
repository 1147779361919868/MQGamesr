# easy_trans

#### 介绍

2021年啦，Mybatis Plus/Jpa的使用越来越多，项目中写SQL越来越少了，有的时候不得已还得写sql，比如：
关联字典，关联其他的表使用外键拿其他表的title/name 等等，为了更优雅的实现id变name/title 字典码变字典描述，easy trans横空出世，通过2个注解就能实现数据翻译，配合自己封装的一些baseService baseController，在配合一些代码生成器插件(比如EasyCode),可真正实现简单的CRUD不写一行代码的目标。

先看效果：
</br>
![enter description here](https://images.gitee.com/uploads/images/2020/0509/105618_248af047_339743.jpeg)

easy trans适用于三种场景
1   我有一个id，但是我需要给客户展示他的title/name  但是我又不想做表关联查询
2   我有一个字典吗 sex  和 一个字典值0  我希望能翻译成   男  给客户展示。
3   我有一组user id 比如 1，2,3  我希望能展示成 张三,李四,王五 给客户

easy trans的三种模式
1  使用redis缓存模式
&nbsp;&nbsp;&nbsp; 一般用于分布式/微服务系统，比如我有用户服务和订单服务，在订单列表中需要展示创建人，他们又不是同一个进程，db也不是同一个，可使用redis 翻译模式  

2  内存缓存(hashmap)模式
&nbsp;&nbsp;&nbsp;  一般用于单体模式，缓存放到hashmap中。

3  非缓存模式
 &nbsp;&nbsp;&nbsp;  非缓存模式不使用缓存，调用 findbyids方法来获取数据用于翻译，一般用于表数据量比较大，缓存扛不住的情况。
 
 


#### 安装教程
1 、先把maven 引用加上
``` xml
       <dependency>
            <groupId>com.fhs-opensource</groupId>
            <artifactId>easy-trans-spring-boot-starter</artifactId>
            <version>1.0.0</version>
        </dependency>
```
2、如果使用Redis请添加redis的引用(如果之前加过了请不要重复添加)
``` xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
```
3、在yaml中添加如下配置
``` YAML
easy-trans:
   autotrans:
       #您的service所在的包 支持通配符比如com.*.**.service.**，他的默认值是com.*.*.service.impl
       package: com.fhs.test.service.** 
	   #启用redis缓存
   is-enable-redis: true
  #yixi 
spring:
  redis:
    host: 192.168.0.213
    port: 6379
    password: 123456
    database: 0
    timeout: 6000
```
4、如果不使用redis，请在启动类加禁用掉redis的自动配置类
``` java
@SpringBootApplication(exclude = { RedisAutoConfiguration.class })
```

#### 使用说明(请务必看完本段)

1、字典翻译使用说明---直接上代码了，可以配合InitializingBean一起玩.</br>
&nbsp;&nbsp;1.1 翻译缓存初始化
``` java
    @Autowired  //注入字典翻译服务
    private  DictionaryTransService dictionaryTransService;
	
	   //在某处将字典缓存刷新到翻译服务中，以下是demo
	    Map<String,String> transMap = new HashMap<>();
        transMap.put("0","男");
        transMap.put("1","女");
        dictionaryTransService.refreshCache("sex",transMap);
```
&nbsp;&nbsp;1.2 字典翻译使用</br>
``` java
   //在对应的字段上 加此注解，type为TransType.DICTIONARY，key为字典分组码
    @Trans(type = TransType.DICTIONARY,key = "sex")
    private Integer sex;
```

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
