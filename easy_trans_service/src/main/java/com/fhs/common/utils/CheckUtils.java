package com.fhs.common.utils;

import com.fhs.common.constant.Constant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * <验证工具类>
 *
 * @author wanglei
 * @version [版本号, 2013年8月7日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class CheckUtils {
    /**
     * <判断对象是否为null或者空>
     *
     * @param obj 需要判断的对象
     * @return 如果对象为null或者空则返回true
     */
    public static boolean isNullOrEmpty(Object obj) {
        if (obj == null || Constant.EMPTY.equals(StringUtil.toString(obj).trim())) {
            return true;
        }
        return false;
    }

    /**
     * 判断对象不为空 适用于字符串或者对象
     *
     * @param obj 需要判断的对象
     * @return 如果对象不为空或者null 返回true
     */
    public static boolean isNotEmpty(Object obj) {
        if (obj == null || Constant.EMPTY.equals(StringUtil.toString(obj).trim())) {
            return false;
        }
        return true;
    }

    /**
     * 判断对象不为空 适用于字符串数组或者对象数组
     *
     * @param objs 需要判断的对象
     * @return 如果对象不为空或者null 返回true
     */
    public static boolean isNotEmpty(Object[] objs) {
        if (objs == null || Stream.of(objs).allMatch(obj -> Constant.EMPTY.equals(StringUtil.toString(obj).trim()))) {
            return false;
        }
        return true;
    }


    /**
     * 判断是否是数字
     *
     * @param obj 需要判断的元素
     * @return 是否是数字
     */
    public static boolean isNumber(Object obj) {

        return checkPattern(StringUtil.toString(obj).trim(), "^[-+]?[0-9]+(\\.[0-9]+)?$");
    }


    /**
     * 判断是否是正数
     *
     * @param obj 需要判断的元素
     * @return 是否是正数
     */
    public static boolean isPositiveNumber(Object obj) {

        return checkPattern(StringUtil.toString(obj).trim(), "^[0-9]+(\\.[0-9]+)?$");
    }

    /**
     * 判断是否是Double类型
     *
     * @param obj 需要判断的元素
     * @return 是否是Double类型(小数点后2位小数)
     */
    public static boolean isDouble(Object obj) {
        return checkPattern(StringUtil.toString(obj).trim(), "^[0-9]+(.[0-9]{1,2})?$");
    }

    /**
     * 判断是否是int类型
     *
     * @param obj 需要判断的元素
     * @return
     */
    public static boolean isInt(Object obj) {
        return checkPattern(StringUtil.toString(obj).trim(), "^[0-9]?$");
    }

    /**
     * 判断集合不为空并且不为null
     *
     * @param con 集合
     * @return 是否为空或者null
     */
    public static boolean checkCollectionIsNullOrEmpty(@SuppressWarnings("rawtypes") Collection con) {
        return con == null || con.size() == 0;
    }

    /**
     * 判断字符串是否满足正则
     *
     * @param str     需要判断的字符串
     * @param pattern 正则
     * @return 判断结果
     */
    public static boolean checkPattern(String str, String pattern) {
        try {
            if (pattern.startsWith("/")) {
                pattern = pattern.substring(1);
            }
            if (pattern.endsWith("/")) {
                pattern = pattern.substring(0, pattern.length() - 1);
            }
            return str.matches(pattern);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 判断字符串是否满足手机正则
     *
     * @param str 手机号
     * @return 判断结果
     */
    public static boolean checkPhone(String str) {
        // 手机正则
        String phone = "^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\\d{8}$";
        return str.matches(phone);
    }

    /**
     * <判断字符串是否是汉字>
     *
     * @param str 汉字
     * @return 判断结果
     */
    public static boolean checkCharacters(String str) {
        // 汉字正则
        String chineseCharacters = "[\u4e00-\u9fa5]+";
        return str.matches(chineseCharacters);
    }

    /**
     * <判断邮箱格式是否满足条件>
     *
     * @param emailStr 需要校验的邮箱字符串
     */
    public static boolean checkEmail(String emailStr) {
        // 邮箱正则
        String emailRegStr = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        return emailStr.matches(emailRegStr);
    }

    /**
     * 判断用户名格式是否满足条件
     *
     * @param userNameStr 需要校验的用户字符串
     */
    public static boolean checkUserName(String userNameStr) {
        // 用户名正则
        String userNameRegStr = "^([a-zA-Z_]{4,32})$";
        return userNameStr.matches(userNameRegStr);
    }

    /**
     * <判断是否满足指定格式的日期>
     *
     * @param dateStr 需要校验的日期字符串
     * @return
     */
    public static boolean checkDate(String dateStr) {
        if (null == dateStr || "".equals(dateStr)) {
            return false;
        }
        boolean flag = false;
        Pattern pattern0 = null;
        Matcher match0 = null;
        String datePattern = "(" +
                // 第一种情况为月份是大月的有31天。
                "(^\\d{3}[1-9]|\\d{2}[1-9]\\d{1}|\\d{1}[1-9]\\d{2}|[1-9]\\d{3}" + // 年
                "([-/\\._]?)" + // 时间间隔符(-,/,.,_)
                "(10|12|0?[13578])" + // 大月
                "([-/\\._]?)" + // 时间间隔符(-,/,.,_)
                "((3[01]|[12][0-9]|0?[1-9])?)" + // 日(31)要验证年月因此出现0/1次
                "([\\s]?)" + // 空格
                "((([0-1]?[0-9]|2[0-3]):([0-5]?[0-9]):([0-5]?[0-9]))?))$" + // 时分秒
                "|" + // 或
                // 第二种情况为月份是小月的有30天，不包含2月。
                "(^\\d{3}[1-9]|\\d{2}[1-9]\\d{1}|\\d{1}[1-9]\\d{2}|[1-9]\\d{3}" + // 年
                "([-/\\._]?)" + // 时间间隔符(-,/,.,_)
                "(11|0?[469])" + // 小月不含2月
                "([-/\\._]?)" + // 时间间隔符(-,/,.,_)
                "(30|[12][0-9]|0?[1-9])" + // 日(30)
                "([\\s]?)" + // 空格
                "((([0-1]?[0-9]|2[0-3]):([0-5]?[0-9]):([0-5]?[0-9]))?))$" + // 时分秒
                "|" + // 或
                // 第三种情况为平年月份是2月28天的。
                "(^\\d{3}[1-9]|\\d{2}[1-9]\\d{1}|\\d{1}[1-9]\\d{2}|[1-9]\\d{3}" + // 年
                "([-/\\._]?)" + // 时间间隔符(-,/,.,_)
                "(0?2)" + // 平年2月
                "([-/\\._]?)" + // 时间间隔符(-,/,.,_)
                "(2[0-8]|1[0-9]|0?[1-9])" + // 日(28)
                "([\\s]?)" + // 空格
                "((([0-1]?[0-9]|2[0-3]):([0-5]?[0-9]):([0-5]?[0-9]))?))$" + // 时分秒
                "|" + // 或
                // 第四种情况为闰年月份是2月29天的。
                // 可以被4整除但不能被100整除的年份。
                // 可以被400整除的数亦是能被100整除，因此后两位是00，所以只要保证前两位能被4整除即可。
                "(^((\\d{2})(0[48]|[2468][048]|[13579][26]))|((0[48]|[2468][048]|[13579][26])00)" + "([-/\\._]?)" + "(0?2)"
                + "([-/\\._]?)" + "(29)" + "([\\s]?)" + "((([0-1]?\\d|2[0-3]):([0-5]?\\d):([0-5]?\\d))?))$" + // 时分秒
                ")";
        ;

        pattern0 = Pattern.compile(datePattern);
        match0 = pattern0.matcher(dateStr);
        flag = match0.matches();
        return flag;
    }

    /**
     * <判断是否满足指定格式的身份证号码>
     *
     * @param idCardStr 需要校验的身份证号码的字符串
     * @return
     */
    public static boolean checkIdCard(String idCardStr) {
        // 身份证号码正则
        String idCardReg = "(^\\d{18}$)|(^\\d{15}$)";
        return idCardStr.matches(idCardReg);
    }

    public static void main(String[] args) {
        boolean result = checkDate("2016/01/-08 ");
        System.out.println(result);
    }


    /**
     * 效验固话
     *
     * @param fixedPhone 固话号码
     * @return 效验结果
     */
    public static boolean checkFixedPhone(String fixedPhone) {
        String reg = "(?:(\\(\\+?86\\))(0[0-9]{2,3}\\-?)?([2-9][0-9]{6,7})+(\\-[0-9]{1,4})?)|" +
                "(?:(86-?)?(0[0-9]{2,3}\\-?)?([2-9][0-9]{6,7})+(\\-[0-9]{1,4})?)";
        return Pattern.matches(reg, fixedPhone);
    }

    /**
     * 校验sql是否有关键字
     *
     * @param str sql字符串
     * @return true 代表有 false字表没有
     */
    public static boolean checkSQL(String str) {
        // 统一转为小写
        str = str.toLowerCase();
        String badStr = "\'|\"|and|exec|execute|insert|select|delete|update|count|drop|*|%|chr|mid|master|truncate|"
                + "char|declare|sitename|net user|xp_cmdshell|;|or|,|like'|and|exec|execute|insert|create|drop|"
                + "table|from|grant|use|group_concat|column_name|xp_shell|(|)|"
                + "information_schema.columns|table_schema|union|where|select|delete|update|order|count|*|"
                + "chr|mid|master|truncate|char|declare|--|+|,|like|//|/|%|#";// 过滤掉的sql关键字，可以手动添加
        String[] badStrs = badStr.split("\\|");
        for (int i = 0; i < badStrs.length; i++) {
            // 循环检测，判断在请求参数当中是否包含SQL关键字
            if (str.indexOf(badStrs[i]) >= 0 || str.indexOf("\\|\\|") >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 利用正则表达式判断对象是否是数字
     *
     * @param obj 对象
     * @return 是否是数字 是 true 不是 false
     */
    public boolean isNumeric(Object obj) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(ConverterUtils.toString(obj));
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }
}
