package com.fhs.common.utils;

import com.fhs.core.trans.util.ReflectUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Title: 字符串格式化类
 * </p>
 * <p>
 * Description: 包装了DispatchAction类
 * </p>
 *
 * @author yan_jwei
 * @version 1.0
 * @created at 2006-01-17
 */

/**
 * <一句话功能简述> <功能详细描述>
 *
 * @author jackwong
 * @version [版本号, 2016年10月31日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
@Slf4j
public class StringUtil {


    // 在字符串左侧补齐空格
    public final static int LEFT_SPACE = 0;

    // 在字符串右侧补齐空格
    public final static int RIGHT_SPACE = 1;

    // 如果字符串实际长度超出指定长度，将左侧截断
    public final static int TRUNC_LEFT = 0;

    // 如果字符串实际长度超出指定长度，将右侧截断
    public final static int TRUNC_RIGHT = 1;

    /**
     * <剔除特殊字符>
     *
     * @param text
     * @param split
     * @return
     */
    public static String removeSpecialchar(String text, String split) {
        if (null != text && !"".equals(text.trim())) {
            return text.replaceAll(split, "");
        }
        return "";

    }

    /**
     * <分割字符串>
     *
     * @param split
     * @param content
     * @return
     */
    public static List<String> getContentBySplit(String split, String content) {
        List<String> list = new ArrayList<String>();
        if (content.contains(split)) {
            String[] htmStr = content.split(split);
            for (String htm : htmStr) {
                list.add(htm);
            }

        }
        return list;
    }

    /**
     * <查找是否存在关键字>
     *
     * @param keyword
     * @param content
     * @return
     */
    public String getContentByKeyWord(String keyword, String content) {
        if (content.contains(keyword)) {
            return content;
        } else {
            return "";
        }
    }

    /**
     * <前后截断获取中间字符串>
     *
     * @param head
     * @param foot
     * @param content
     * @return
     */
    public String getContentByCut(String head, String foot, String content) {
        int beginIndex = content.indexOf(head);
        int diff = head.length();
        int endIndex = content.indexOf(foot);
        String htmStr = "";
        if (beginIndex > 0 && endIndex > 0) {
            htmStr = content.substring(beginIndex + diff, endIndex);
        }
        return htmStr;

    }

    /**
     * <除去转义符号和空字符>
     *
     * @param src
     * @return
     */
    public static String removeEsc(String src) {
        String string =
                src.replaceAll("\\\\/", "/").replaceAll("\\\\\"", "\"").replaceAll("\\\\t", "").replaceAll("\\\\n", "");
        return string;
    }

    /**
     * <Unicode转中文> <功能详细描述>
     *
     * @param dataStr
     * @return
     */
    public static String decodeUnicode(final String dataStr) {
        int start = 0;
        int end = 0;
        final StringBuffer buffer = new StringBuffer();
        while (start > -1) {
            start = dataStr.indexOf("\\u", start);
            if (start == -1) {
                buffer.append(dataStr.substring(end));
            } else {
                buffer.append(dataStr.substring(end, start));
                end = start + 6;

                String charStr = "";
                charStr = dataStr.substring(start + 2, end);
                char letter = '0';
                try {
                    letter = (char) Integer.parseInt(charStr, 16);
                    buffer.append(new Character(letter).toString());
                } catch (NumberFormatException e) {
                    buffer.append("\\u" + charStr);
                }
                start = end;
            }
        }
        return buffer.toString();
    }

    /**
     * <判断字符串是否为空>
     *
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        if (str == null || "".equals(str.trim())) {
            return true;
        }

        return false;
    }

    /**
     * 该方法去掉字符串的左边空格
     *
     * @param str 需要去掉左边空格的字符串
     * @return String 已经去掉左边空格的字符串
     */
    public static String leftTrim(String str) {
        if (str == null || "" == str) {
            return "";
        }

        byte[] bytes = {};
        try {
            bytes = str.getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        int index = 0;
        byte ch;
        do {
            ch = bytes[index];
            if (ch != ' ') {
                break;
            }
            index++;
        } while (true);
        return str.substring(index);
    }

    /**
     * 改方法去掉字符串的右边空格
     *
     * @param str 需要去掉右边空格的字符串
     * @return String 已经去掉右边空格的字符串
     */
    public static String rightTrim(String str) {
        if (str == null) {
            return "";
        }

        byte[] bytes = {};
        try {
            bytes = str.getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }

        int index = StringUtil.length(str);

        if (index == 0) {
            return "";
        }

        index = index - 1;
        byte ch;
        do {
            ch = bytes[index];
            if (ch != ' ') {
                break;
            }
            index--;
        } while (index >= 0);

        return str.substring(0, index + 1);
    }

    /**
     * <将字符串中所有的空格删除，包括左边、右边、中间。>
     *
     * @param str
     * @return
     */
    public static String allTrim(String str) {
        if (str == null) {
            return "";
        }
        String tmp = str.trim();
        if (tmp.equals("")) {
            return "";
        }
        int idx = 0;
        int len = 0;
        len = tmp.length();
        idx = tmp.indexOf(" ");
        while (idx > 0) {
            tmp = tmp.substring(0, idx) + tmp.substring(idx + 1, len);
            idx = tmp.indexOf(" ");
            len = tmp.length();
        }
        return tmp;
    }

    /**
     * 该方法首先判断传入的字符串是否是null,如果是null则返回"", 否则对传入的字符串执行trim操作后返回trim的结果
     *
     * @param orin 需要进行处理的字符串
     * @return String 处理完成的结果字符串
     */
    public static String trim(String orin) {
        return (null == orin) ? "" : orin.trim();
    }

    /**
     * <该方法计算字符串(包括中文)的实际长度.>
     *
     * @param str 需要计算长度的字符串
     * @return int 字符串的实际长度
     */
    public static int length(String str) {

        if (str == null) {
            return 0;
        }
        try {
            return new String(str.getBytes("GBK"), "8859_1").length();
        } catch (UnsupportedEncodingException e) {
            return -1;
        }
    }

    /**
     * < 使用GBK字符集将此 String 编码为 byte 序列，并将结果存储到一个新的 byte 数组中。>
     *
     * @param str
     * @return
     */
    public static byte[] getBytes(String str) {
        try {
            return str.getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            return new byte[]{};
        }
    }

    /**
     * <检查SQL中的动态字符值中是否存在'号,如果存在， 处理该值并返回处理后的字符串，使其可以在SQL语句中使用>
     *
     * @param str
     * @return
     */
    public static String checkSqlValue(String str) {
        if (str == null) {
            return str;
        }
        String cstr = str;
        int pos = cstr.indexOf("'");

        while (pos >= 0) {
            cstr = cstr.substring(0, pos + 1) + "'" + cstr.substring(pos + 1);
            pos = cstr.indexOf("'", pos + 2);
        }
        return cstr;
    }

    /**
     * <分割字符串>
     *
     * @param str    被分割字符串
     * @param delim  分割符
     * @param length 分割后的字符串数量
     * @return 分割后的字符串数组
     */
    public static String[] stringTokenizer(String str, String delim, int length) {
        StringTokenizer stk = new StringTokenizer(str, delim);
        String[] returnStr = new String[length];
        int i = 0;
        while (stk.hasMoreTokens()) {
            returnStr[i] = stk.nextToken();
            i++;
        }
        return returnStr;
    }

    /**
     * <分割字符串>
     *
     * @param str   被分割字符串
     * @param delim 分割符
     * @return 分割后的字符串List
     */
    public static List<String> stringTokenizer(String str, String delim) {
        StringTokenizer stk = new StringTokenizer(str, delim);
        List<String> returnStr = new ArrayList<String>();
        while (stk.hasMoreTokens()) {
            returnStr.add(stk.nextToken());
        }
        return returnStr;
    }

    /**
     * <判断两字符串trim后是否相等>
     *
     * @param src
     * @param dest
     * @return
     */
    public static boolean isEqualAfterTrim(String src, String dest) {
        if (null == src || null == dest) {
            return false;
        }
        return trim(src).equals(trim(dest));

    }

    /**
     * <根据获取的字段名称获取对应get/set方法>
     *
     * @return
     */
    public static String getMethodName(String str, String filed) {
        String frist = filed.substring(0, 1);
        String temp = filed.replaceFirst(frist, frist.toUpperCase());
        return str + temp;
    }

    /**
     * <传递类型获取返回页面>
     *
     * @param type
     * @return
     */
    public static String getResult(String type) {
        StringBuffer sb = new StringBuffer(type);
        sb.append("_show");
        return sb.toString();
    }

    /**
     * <重写tostring方法>
     *
     * @param obj 需要转换为string的对象
     * @return
     */
    public static String toString(Object obj) {
        if (obj != null) {
            return String.valueOf(obj);
        }
        return "";
    }

    /**
     * <重写tostring方法>
     *
     * @param obj        需要转换为string的对象
     * @param defaultVal 默认值
     * @return
     */
    public static String toString(Object obj, String defaultVal) {
        if (obj != null) {
            return String.valueOf(obj);
        }
        return defaultVal;
    }




    /**
     * 将id类型的list用","拼接
     *
     * @param list 字符串集合
     * @return 处理后的字符串
     */
    public static String getStrForIntegerIn(List<String> list) {
        return getStrForIn(list, false);
    }

    /**
     * 获取字符串for in
     *
     * @param collection 集合
     * @param isAddMarks 是否添加引号
     * @return 可以直接in的字符串
     */
    public static String getStrForIn(Collection collection, boolean isAddMarks) {
        if(collection == null || collection.isEmpty()){
            return "";
        }
        StringBuilder result = new StringBuilder();
        String appendMarks = isAddMarks ? "'" : "";
        for (Iterator<?> iter = collection.iterator(); iter.hasNext(); ) {
            result.append(",").append(appendMarks + ConverterUtils.toString(iter.next()) + appendMarks);
        }
        return result.length() > 0 ? result.substring(1) : "-10000";
    }

    /**
     * 传一个list bean 和一个字段，此方法吧字段集合用逗号分隔 转换为一个字符串返回
     *
     * @param list     beanlist
     * @param fileName 字段名称
     * @return bean1.get字段 + 逗号bean2.get字段
     */
    public static String getStrForIntegerIn(List<? extends Object> list, String fileName) {
        List<String> strList = new ArrayList<>();
        for (Object obj : list) {
            strList.add(StringUtil.toString(ReflectUtils.getValue(obj, fileName)));
        }
        return getStrForIn(strList, false);
    }


    /**
     * <去掉第一个字符和最后一个字符>
     *
     * @param str 字符串
     * @return 处理后的字符串
     */
    public static String getStrIn(String str) {
        if (str == null || str.length() < 3) {
            return "0";
        }
        str = str.substring(1);
        str = str.substring(0, str.length() - 1);
        return str;
    }

    /**
     * 返回一个UUID
     *
     * @return uuid
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 验证字符串是否包含中文
     *
     * @param param
     * @return 包含中文返回 ：true
     */
    public static boolean validtIsChinese(String param) {
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z_]+$");
        Matcher matcher = pattern.matcher(param);
        return !matcher.matches();
    }

    /**
     * 解码中文
     *
     * @param str
     * @return
     */
    public static String decodeString(String str) {
        return str;
    }

    /**
     * 判断字符串是否是整数，如果为空或者""，返回false
     *
     * @param str
     * @return
     */
    public static boolean isInteger(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        return Pattern.compile("^\\d+$").matcher(str).matches();
    }

    public static String deleteLastSubstr(String oldStr, String subStr) {
        int ogrStrLength = oldStr.length();
        int subStrLength = subStr.length();
        if (oldStr.endsWith(subStr)) {
            oldStr = oldStr.substring(0, ogrStrLength - subStrLength);
        }
        return oldStr;
    }

    /**
     * 用0格式化数字
     *
     * @param prefix 前缀，可不加
     * @param format 格式化格式（%04d 四位数字前面补0）
     * @param count  需要格式化的数字
     * @return
     */
    public static String formatCountWith0(String prefix, String format, Integer count) {
        String str = prefix + String.format(format, count);
        return str;
    }

    /**
     * 格式化字符串如果为null即引用地址为空则返回空字符串
     *
     * @param string
     * @return
     */
    public static String formatString2empty(String string) {
        String returnString = "";
        if (string == null) {
            return returnString;
        }
        return string;
    }

    /**
     * 数字金额大写转换，思想先写个完整的然后将如零拾替换成零 要用到正则表达式
     */
    public static String digitUppercase(double num) {
        String digit[] = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String unit1[] = {"", "拾", "佰", "仟"};// 把钱数分成段,每四个一段,实际上得到的是一个二维数组
        String unit2[] = {"圆", "万", "亿", "万亿"}; // 把钱数分成段,每四个一段,实际上得到的是一个二维数组

        DecimalFormat df = new DecimalFormat("0.00");
        String value = df.format(Math.round(num * 100) / 100.0);
        String[] value1 = value.split("\\.");
        String head = value1[0]; // 整数部分
        String end = value1[1]; // 小数部分
        String endMoney = "";
        String headMoney = "";
        if ("00".equals(end)) {
            endMoney = "整";
        } else {
            if (!end.substring(0, 1).equals("0")) {
                endMoney += digit[Integer.valueOf(end.substring(0, 1))] + "角";
            } else if (end.substring(0, 1).equals("0") && !end.substring(1, 2).equals("0")) {
                endMoney += "零";
            }
            if (!end.substring(1, 2).equals("0")) {
                endMoney += digit[Integer.valueOf(end.substring(1, 2))] + "分";
            }
        }
        char[] chars = head.toCharArray();
        Map<String, Boolean> map = new HashMap<String, Boolean>();// 段位置是否已出现zero
        boolean zeroKeepFlag = false;// 0连续出现标志
        int vidxtemp = 0;
        for (int i = 0; i < chars.length; i++) {
            int idx = (chars.length - 1 - i) % 4;// 段内位置 unit1
            int vidx = (chars.length - 1 - i) / 4;// 段位置 unit2
            String s = digit[Integer.valueOf(String.valueOf(chars[i]))];
            if (!"零".equals(s)) {
                headMoney += s + unit1[idx] + unit2[vidx];
                zeroKeepFlag = false;
            } else if (i == chars.length - 1 || map.get("zero" + vidx) != null) {
                headMoney += "";
            } else {
                headMoney += s;
                zeroKeepFlag = true;
                map.put("zero" + vidx, true);// 该段位已经出现0；
            }
            if (vidxtemp != vidx || i == chars.length - 1) {
                headMoney = headMoney.replaceAll(unit2[vidx], "");
                headMoney += unit2[vidx];
            }
            if (zeroKeepFlag && (chars.length - 1 - i) % 4 == 0) {
                headMoney = headMoney.replaceAll("零", "");
            }
        }
        String upperName = headMoney + endMoney;
        if (upperName.equals("圆整")) {
            return "零圆整";
        } else if (upperName.startsWith("圆零")) {
            return upperName.substring(2);
        } else if (upperName.startsWith("圆")) {
            return upperName.substring(1);
        }
        return upperName;
    }

    /**
     * 用分隔符连接数据
     */
    public static String join(Collection<?> coll, String split, String defaultStr) {
        if (coll == null || coll.isEmpty()) {
            return defaultStr;
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Object s : coll) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(split);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * 用分隔符连接数据
     */
    public static String join(Collection<?> coll, String split) {
        return join(coll, split, "");
    }

    /**
     * 用分隔符连接数据
     */
    public static String join(String[] array, String split) {
        if (array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(split);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }

    /**
     * 截取标题。用于APP显示。只显示10个字符，多余的用...代替
     *
     * @param str
     * @return
     */
    public static String subNoticeTitle(String str) {
        if (str == null || "".equals(str)) {
            return "";
        } else {
            if (str.length() > 10) {
                str = str.substring(0, 10) + "...";

            }
        }
        return str;
    }

    /**
     * <格式化字符串用单引号包裹>
     *
     * @param str
     * @return 返回单引号包裹之后的字符串
     */
    public static String useQuotesWrapValue(Object str) {
        if (str == null || "".equals(str)) {
            return "''";
        } else {
            str = "'" + str + "'";
        }
        return str.toString();
    }

    /**
     * 截取前面的
     *
     * @param split
     * @param content
     * @return 截取后的字符串
     */
    public static String matchFront(String split, String content) {
        String front = "";
        if (!CheckUtils.isNullOrEmpty(content)) {
            Integer endIndex = content.indexOf(split);
            front = content.substring(0, endIndex);
        }
        return front;
    }

    /**
     * 截取后面的
     *
     * @param split
     * @param content
     * @return 截取后的字符串
     */
    public static String matchBehind(String split, String content) {
        String behind = "";
        if (!CheckUtils.isNullOrEmpty(content)) {
            Integer endIndex = content.indexOf(split);
            behind = content.substring(endIndex + 1);
        }
        return behind;
    }

    /**
     * <去除字符串的第一个和最后一个特定字符 如果存在>
     *
     * @param str      需要进行去除的字符串
     * @param splitStr 需要去除的字符串
     * @return 返回去除前后指定字符的字符串
     */
    public static String trimAllSplit(String str, String splitStr) {
        if (str.startsWith(splitStr)) {
            str = str.substring(1);
        }
        if (str.endsWith(splitStr)) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * 转成list<int>
     *
     * @param str (多个id 用逗号隔开)
     * @return
     */
    public static List<Integer> StringToInt(String str) {
        String[] arrs = str.split(",");
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < arrs.length; i++) {
            result.add(ConverterUtils.toInt((arrs[i])));
        }
        return result;
    }

    /**
     * 对文件名格式化下，去除非法字符
     *
     * @param fileName
     * @return
     */
    public static String formatFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String fileNameInvaildPattern = "[\\/:\\*\\?\"<>|]";
        String vaildFileName = fileName.replaceAll(fileNameInvaildPattern, "");
        return vaildFileName;
    }

    /**
     * 格式化订单号
     *
     * @param date   yyyyMMdd
     * @param number
     * @return 20位的订单号
     */
    public static String formatOrderNumber(String date, int number) {
        return formatCountWith0(date, "%08d", number);
    }

    /**
     * 格式化序列号
     *
     * @param number
     * @return 6位的序列号
     */
    public static Integer formatShortNumber(int number) {
        int source = 100000;
        int target = Integer.valueOf(formatCountWith0("", "%06d", number));
        return source + target;
    }

    /**
     * 格式化订单号
     *
     * @param number
     * @return 4位的订单号
     */
    public static String formatCheckNumber(int number) {
        return formatCountWith0("", "%04d", number);
    }

    /**
     * 获取驼峰名称，首字母大写
     *
     * @param str
     * @return
     */
    public static String getHumpName(String str) {
        boolean isDist = false;
        StringBuilder sbr = new StringBuilder();
        for (char char1 : str.toCharArray()) {
            if (char1 == '_') {
                isDist = true;
                continue;
            } else {
                sbr.append(isDist && 'a' <= char1 && 'z' >= char1 ? (char) (char1 - 32) : char1);
                isDist = false;
            }
        }
        return sbr.toString();
    }

    /**
     * 首字母大写
     *
     * @param str 需要首字母大写的字符串
     * @return 首字母大写
     */
    public static String firstCharUpperCase(String str) {
        return str.substring(0, 1).toUpperCase().concat(str.substring(1));
    }


    /**
     * 转化字符串为十六进制编码
     *
     * @param s 字符串
     * @return 16进制编码
     */
    public static String toHexString(String s, int length) {
        String result = Integer.toHexString(ConverterUtils.toInt(s)).toUpperCase();
        int resultLength = result.length();
        if (resultLength < length) {
            for (int i = 0; i < (length - resultLength); i++) {
                result = "0" + result;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(formatShortNumber(1888));
    }

    //

    /**
     * 转化十六进制编码为字符串
     *
     * @param s 十六进制编码
     * @return 字符串
     */
    public static String toStringHex(String s) {
        return StringUtil.toString(Integer.parseInt(s, 16));
    }
}
