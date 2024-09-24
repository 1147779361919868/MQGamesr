# easy_trans

#### 介绍

2021年啦，Mybatis Plus/Jpa的使用越来越多，项目中写SQL越来越少了，有的时候不得已还得写sql，比如：
关联字典，关联其他的表使用外键拿其他表的title/name 等等，为了更优雅的实现id变name/title 字典码变字典描述，easy trans横空出世，通过2个注解就能实现数据翻译，配合自己封装的一些baseService baseController，在配合一些代码生成器插件(比如EasyCode),可真正实现简单的CRUD不写一行代码的目标。

先看效果：
<br/>
![enter description here](https://images.gitee.com/uploads/images/2020/0509/105618_248af047_339743.jpeg)

easy trans适用于三种场景<br/>
1   我有一个id，但是我需要给客户展示他的title/name  但是我又不想做表关联查询<br/>
2   我有一个字典吗 sex  和 一个字典值0  我希望能翻译成   男  给客户展示。<br/>
3   我有一组user id 比如 1，2,3  我希望能展示成 张三,李四,王五 给客户<br/>

easy trans的三种模式<br/>
1  使用redis缓存模式<br/>
&nbsp;&nbsp;&nbsp; 一般用于分布式/微服务系统，比如我有用户服务和订单服务，在订单列表中需要展示创建人，他们又不是同一个进程，db也不是同一个，可使用redis 翻译模式 <br/> 

2  内存缓存(hashmap)模式<br/>
&nbsp;&nbsp;&nbsp;  一般用于单体模式，缓存放到hashmap中。<br/>

3  非缓存模式<br/>
 &nbsp;&nbsp;&nbsp;  非缓存模式不使用缓存，调用 findbyids方法来获取数据用于翻译，一般用于表数据量比较大，缓存扛不住的情况。<br/>
 
 


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

1、字典翻译使用说明---直接上代码了，可以配合InitializingBean一起玩.<br/>
&nbsp;&nbsp;1.1 翻译缓存初始化<br/>
``` java
    @Autowired  //注入字典翻译服务
    private  DictionaryTransService dictionaryTransService;
	
	   //在某处将字典缓存刷新到翻译服务中，以下是demo
	    Map<String,String> transMap = new HashMap<>();
        transMap.put("0","男");
        transMap.put("1","女");
        dictionaryTransService.refreshCache("sex",transMap);
```
&nbsp;&nbsp;1.2 字典翻译使用<br/>
``` java
   //在对应的字段上 加此注解，type为TransType.DICTIONARY，key为字典分组码
    @Trans(type = TransType.DICTIONARY,key = "sex")
    private Integer sex;
```

2、AutoTrans（除了字典外的其他表翻译）使用说明---直接上代码了，可以配合InitializingBean一起玩.<br/>
&nbsp;&nbsp;2.1 service实现类改动，主要2个点1是添加AutoTrans注解，2 是实现AutoTransAble 接口<br/>
``` java
@Service
@AutoTrans(namespace = "teacher",fields = "name",defaultAlias = "teacher",useCache = true,useRedis = true)  //namespace = 表别名  fields = 哪些字段需要出现在翻译结果中这里写了name defaultAlias =默认别名，比如我这里有个name字段别的表也有个name字段，为了区分这里配置为teacher 在翻译结果中 就会出现teacherName 而不是name  useCache = 是否使用缓存  useRedis = 是否使用redis缓存
public class TeacherService implements AutoTransAble {

   //在不使用缓存的时候使用，如果transMore的时候会拼接teacherid集合，调用此方法获取id集合对应的teacher对象
     public List<P> findByIds(List<?> ids) {
	    //推荐使用JPA/Mybatis Plus的方法哦
        return this.baseMapper.selectBatchIds(ids);
    }
 
 // 在开启缓存的时候，springboot启动完成后会拿所有数据放到缓存里
    @Override
    public List select() {
      return  this.baseMapper.selectList((Wrapper)null)
    }

// 在不开启缓存的时候，transone会通过此方法获取翻译数据
    @Override
    public VO selectById(Object primaryValue) {
       return this.baseMapper.selectById(primaryValue);
    }
```
以上，建议在baseservice中添加以上几个方法，这样子service就不用每个都写了。

&nbsp;&nbsp;2.2 Autotrans翻译使用</br>
``` java
     //指定翻译的namespace，和翻译类型为TransType.AUTO_TRANS
    @Trans(type = TransType.AUTO_TRANS,key = "teacher")
    private String teacherId;
   //如果有2个teacherid 可以通过namespace#别名  来起别名区分
    @Trans(type = TransType.AUTO_TRANS,key = "teacher#english")
    private String englishteacherId;
```
3、POJO修改 a 实现vo接口(Teacher类也要实现哦)，提供一个transMap，框架会把翻译结果put到这个map中，建议使用basePOJO 的方法来实现

``` java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Student implements VO {

    private String studentName;

    @Trans(type = TransType.AUTO_TRANS,key = "teacher")
    private String teacherId;

    @Trans(type = TransType.AUTO_TRANS,key = "teacher#english")
    private String englishteacherId;

    @Trans(type = TransType.DICTIONARY,key = "sex")
    private Integer sex;

    public Map<String,String> transMap = new HashMap<>();

    @Override
    public Map<String, String> getTransMap() {
        return transMap;
    }
}

```
4、框架中没有使用JPA/Mybatis Plus怎么办

``` java
   //vo中有一个getPkey 方法默认是找@Id 或者 @TableId 标识的字段，如果没有使用JPA/Mybatis Plus 可重写此方法返回表主键的值比如 return this.id;

    @JsonIgnore
    @JSONField(serialize = false)
     default Object getPkey(){
         Field idField = getIdField(true);
         try {
             return idField.get(this);
         } catch (IllegalAccessException e) {
             return null;
         }
     }

```
5、准备工作已经完成，最后一步，使用翻译服务进行翻译
``` java
    @Autowired
    private TransService transService;

    @Test
    public void transOne(){
        Student student = new Student();
        student.setStudentName("张三");
        student.setTeacherId("1");
        student.setEnglishteacherId("2");
        student.setSex(1);
		//翻译一个对象
        transService.transOne(student);
        System.out.println(JsonUtils.bean2json(student));
    }


    @Test
    public void transMore(){
        Student student = new Student();
        student.setStudentName("张三");
        student.setTeacherId("1");
        student.setEnglishteacherId("2");
        student.setSex(1);
        List<Student> studentList = new ArrayList<>();
        studentList.add(student);
		//翻译多个对象
        transService.transMore(studentList);
        System.out.println(JsonUtils.list2json(studentList));
    }
```

6、缓存刷新
&nbsp;&nbsp;6.1 非集群模式下的缓存刷新
调用AutoTransService的refreshCache(Map<String, Object> messageMap) 
map中put一个namespace 为teacher的话，就代表刷新teacher的缓存，如果map中什么都不put代表刷新所有缓存。
&nbsp;&nbsp;6.2 集群模式下的缓存刷新(必须开启redis支持才可以)
``` java
 @Autowired
 private RedisCacheService redisCacheService;
 Map<String, String> message = new HashMap();
            message.put("transType", "auto");
            message.put("namespace", "teacher");
            this.redisCacheService.convertAndSend("trans", JsonUtils.map2json(message));
```
7、DEMO
https://gitee.com/fhs-opensource/easy_trans_springboot_demo

#### 参与贡献

1.  如果遇到使用问题可以加QQ群:976278956

#### 写到最后

easy trans是从fhs framework剥离出来的项目，原来是作者一个快速开发平台里面的功能，考虑到很多公司都是用自己搭建的项目或者其他的快速开发平台，作者也想提供一种便利的方式让此功能集成到其他的现有平台中，方作出决定进行剥离，有任何想法都可以加群联系。