package com.fhs.common.constant;

/**
 * <公共常量接口>
 *
 * @author wanglei
 * @version [版本号, 2013年8月7日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public interface Constant {
    /**
     * 禁用
     */
    Integer DISABLE = 0;
    /**
     * 启用
     */
    Integer ENABLED = 1;
    /**
     * 验证码生成几位参数
     */
    int VERIFYING_CODE_NUM = 5;


    /**
     * 0
     */
    int ZREO = 0;

    /**
     * 是
     */
    int INT_TRUE = 1;

    /**
     * 否
     */
    int INT_FALSE = 0;
    /**
     * 是
     */
    String STR_YES = "1";
    /**
     * 否
     */
    String STR_NO = "0";

    /**
     * 默认session类型
     */
    int DEFAULT_SESSION_TYPE = -1;

    /**
     * HTTP请求调用失败
     */
    int HTTP_ERROR = 300;

    /**
     * session中的用户
     */
    String SESSION_USER = "sessionUser";

    /**
     * 用户数据权限
     */
    String SESSION_USER_DATA_PERMISSION = "sessionUserDataPermission";

    /**
     * page 第几页
     */
    String PAGE = "page";

    /**
     * 每页多少条，主要给我的物业手机app用
     */
    String PAGE_SIZE = "pageSize";

    /**
     * page 每页多少条数据或者数据
     */
    String ROWS = "rows";

    /**
     * 总数
     */
    String TOTAL = "total";

    /**
     * datagrid底部
     */
    String FOOTER = "footer";

    /**
     * 大
     */
    int BIGGER = 1;

    /**
     * 小
     */
    int SMALL = -1;

    /**
     * 等于
     */
    int EQUAL = 0;

    /**
     * 已经失效/过期的界面
     */
    String OLD = "old";

    /**
     * 文件ids
     */
    String FILE_IDS = "fileIds";

    /**
     * 比例
     */
    String RATIO = "ratio";


    /**
     * start
     */
    String START = "start";

    /**
     * end
     */
    String END = "end";


    /**
     * 接口调用成功代码
     */
    int SUCCESS_CODE = 200;

    /**
     * 接口调用失败代码
     */
    int DEFEAT_CODE = 300;

    /**
     * 302
     */
    int CODE_302 = 302;

    /**
     * sessionId/ssoid过期
     */
    int SESSION_ID_TIMEOUT = 403;

    /**
     * 服务器错误
     */
    int SERVER_EXCEPTION = 500;

    /**
     * 检查中
     */
    String CHECK_ING = "2";

    /**
     * 检查验证成功
     */
    String CHECK_SUCCESS = "3";

    /**
     * true
     */
    Boolean BTRUE = true;

    /**
     * 重新加载
     */
    String RELOAD = "reload";

    /**
     * false
     */
    Boolean BFALSE = false;

    /**
     * 是
     */
    String STR_TRUE = "true";


    /**
     * 文件名字
     */
    String FILENAME = "fileName";

    /**
     * 状态
     */
    String STATUS = "status";


    /**
     * 编辑
     */
    String EDIT = "edit";

    /**
     * 更新
     */
    String UPDATE = "update";

    /**
     * 最大的int值
     */
    int MAX_INT = 2147483647;

    /**
     * 查看 预览
     */
    String VIEW = "view";


    /**
     * 斜杠
     */
    String SLASH = "/";

    /**
     * EMPTY 空字符串
     */
    String EMPTY = "";

    /**
     * 中划线
     */
    String CENTER_LINE = "-";

    /**
     * 字典
     */
    String WORD_BOOK = "wordbook";

    /**
     * 用户信息
     */
    String USER_INFO = "sysUser";

    /**
     * 1
     */
    int ONE = 1;

    /**
     * 2
     */
    int TWO = 2;

    /**
     * 3
     */
    int THREE = 3;

    /**
     * 0
     */
    int ZERO = 0;

    /**
     * 5
     */
    int FIVE = 5;

    /**
     * ID
     */
    String ID = "id";

    /**
     * 创建日期
     */
    String CREATE_DATE = "create_date";

    /**
     * excel后缀
     */
    String XLSX = ".xlsx";

    /**
     * 是
     */
    int I_YES = 1;

    void test();


    /**
     * 分号
     */
    String SEMICOLON = ";";

    /**
     * 冒号
     */
    String COLON = ":";

    /**
     * list
     */
    String LIST = "list";


    /**
     * 验证码过期
     */
    int CODE_OVERDUE = 201;


    /**
     * 年
     */
    String YEAR = "year";

    /**
     * 月
     */
    String MONTH = "month";

    /**
     * 日
     */
    String DAY = "day";

    /**
     * 时
     */
    String HOURS = "hours";

    /**
     * 分
     */
    String MINUTES = "minutes";

    /**
     * 秒
     */
    String SECONDS = "seconds";

    /**
     * 毫秒
     */
    String MILLISECOND = "millisecond";


    /**
     * 日期格式化字符串年月日格式
     */
    String DATE_FORMAT_Y_M_D = "yyyy-MM-dd";

    /**
     * 日期格式化字符串时分格式
     */
    String DATE_FORMAT_H_M = "HH:mm";

    /**
     * 日期格式化字符串年月日时分格式
     */
    String DATE_FORMAT_Y_M_D_H_M = "yyyy-MM-dd HH:mm";

    /**
     * 日期格式化字符串年月日时分秒格式
     */
    String DATE_FORMAT_Y_M_D_H_M_S = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式化字符串时分秒格式
     */
    String DATE_FORMAT_H_M_S = "HH:mm:ss";

    /**
     * 等待执行
     */
    int TO_BE_RUN = 0;

    /**
     * 获取数据成功
     */
    int GET_DATA_SUCCESS = 1;

    /**
     * 获取数据成功
     */
    int GET_DATA_FAIL = 2;

    /**
     * 用于查询所有数据
     */
    int PAGE_ALL = -1;

    /**
     * 订单一次生成个数
     */
    int ONCE_ORDER_NUM_CREATE = 500;

    /**
     * vue模式在header中带的key
     */
    String VUE_HEADER_TOKEN_KEY = "Authorization";

    /**
     * vue菜单
     */
    String MENU_TYPE_VUE = "3";

    /**
     * 已删除
     */
    int YES_DELETE = 1;
    /**
     * 未删除
     */
    int NO_DELETE = 0;

    /**
     * 软删除字段
     */
    String DEL_FIELD = "deleted";

}
