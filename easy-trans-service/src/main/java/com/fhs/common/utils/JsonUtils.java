package com.fhs.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Json工具类
 *
 * @author wanglei
 * @version [版本号, 2015年9月23日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class JsonUtils {
    private static SerializeConfig mapping = new SerializeConfig();


    /**
     * <把任意对象转换为json字符串 >
     *
     * @param obj 任意对象
     * @return json字符串
     */
    public static String object2json(Object obj) {
        if (obj == null) {
            return "{}";
        }
        return JSON.toJSONString(obj, mapping, SerializerFeature.WriteMapNullValue);
    }


    /**
     * 去掉value是空的key
     *
     * @param json json
     * @return 处理后的json
     */
    public static String clearNullAttr(String json) {
        String result = "";
        if (!(json.equals(""))) {
            LinkedHashMap<String, Object> jsonMap =
                    JSON.parseObject(json, new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            JSONObject jsonObject = new JSONObject(true);
            jsonObject.putAll(jsonMap);
            Iterator<String> it = jsonObject.keySet().iterator();
            while (it.hasNext()) {
                Object keyTemp = it.next();
                if (keyTemp == null) {
                    jsonObject.remove(keyTemp);
                }
            }
            if (jsonObject != null) {
                result = JSON.toJSONString(jsonObject);
            }
        }
        return result;
    }

    /**
     * linkedMap2Json 转json
     *
     * @param map linkedMap  linkedMap
     * @return json json字符串
     */
    public static String linkedMap2Json(LinkedHashMap<? extends String, ? extends Object> map) {
        JSONObject jsonObject = new JSONObject(true);
        jsonObject.putAll(map);
        return jsonObject.toJSONString();
    }

    /**
     * 先对map的key排序(字典序)，然后转换为json返回
     *
     * @param param 参数map
     * @return json
     */
    public static String getSortKeyJson(Map<String, Object> param) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(param.keySet());
        Collections.sort(keys);
        int size = keys.size();
        for (int i = 0; i < size; i++) {
            result.put(keys.get(i), param.get(keys.get(i)));
        }
        return JsonUtils.linkedMap2Json(result);
    }

    public static Map<String, Integer> getKeyIndex(Map<String, Object> param) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(param.keySet());
        Collections.sort(keys);
        int size = keys.size();
        for (int i = 0; i < size; i++) {
            result.put(keys.get(i), i);
        }
        return result;
    }

    /**
     * <把bean对象转换为json字符串>
     *
     * @param bean bean对象
     * @return json字符串
     */
    public static String bean2json(Object bean) {
        return object2json(bean);
    }


    /**
     * <把list对象转换为json字符串>
     *
     * @param list list对象
     * @return json字符串
     */
    public static String list2json(List<?> list) {
        if (list == null) {
            return "[]";
        }
        return object2json(list);
    }

    /**
     * <把数组对象转换为json字符串>
     *
     * @param array 数组对象
     * @return json字符串
     */
    public static String array2json(Object[] array) {
        if (array == null) {
            return "[]";
        }
        return object2json(array);
    }

    /**
     * <把map对象转换为json字符串>
     *
     * @param map map对象
     * @return json字符串
     */
    public static String map2json(Map<?, ?> map) {
        return object2json(map);
    }

    /**
     * <把set对象转换为json字符串>
     *
     * @param set set对象
     * @return json字符串
     */
    public static String set2json(Set<?> set) {
        if (set == null) {
            return "[]";
        }
        return object2json(set);
    }

    /**
     * <把String对象转换为json字符串>
     *
     * @param s String对象
     * @return json字符串
     */
    public static String string2json(String s) {
        if (null == s) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                default:
                    if (ch >= '\u0000' && ch <= '\u001F') {
                        String ss = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            sb.append('0');
                        }
                        sb.append(ss.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * 把json字符串转换为list
     *
     * @param jsonStr
     * @return
     */
    public static List<Map<String, Object>> parseJSON2List(String jsonStr) {
        JSONArray jsonArr = JSONArray.parseArray(jsonStr);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Iterator<Object> it = jsonArr.iterator();
        while (it.hasNext()) {
            JSONObject json2 = JSONObject.parseObject(JSON.toJSONString(it.next(), mapping));
            list.add(parseJSON2Map(json2.toString()));
        }
        return list;
    }

    /**
     * 把json字符串转换为map
     *
     * @param jsonStr json字符串
     * @return map
     */
    public static Map<String, Object> parseJSON2Map(String jsonStr) {
        Map<String, Object> map = new HashMap<String, Object>();
        // 最外层解析
        JSONObject json = JSONObject.parseObject(jsonStr);
        for (Object k : json.keySet()) {
            Object v = json.get(k);
            // 如果内层还是数组的话，继续解析
            if (v instanceof JSONArray) {
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                Iterator<Object> it = ((JSONArray) v).iterator();
                while (it.hasNext()) {
                    JSONObject json2 = JSONObject.parseObject(JSON.toJSONString(it.next(), mapping));
                    list.add(parseJSON2Map(json2.toString()));
                }
                map.put(k.toString(), list);
            } else {
                map.put(k.toString(), v);
            }
        }
        return map;
    }


    /**
     * 格式化json字符串(主要是用于输出打印的时候使用)
     *
     * @param jsonStr json字符串
     * @return 格式化后的json字符串
     */
    public static String format(String jsonStr) {
        int level = 0;
        StringBuffer jsonForMatStr = new StringBuffer();
        for (int i = 0; i < jsonStr.length(); i++) {
            char c = jsonStr.charAt(i);
            if (level > 0 && '\n' == jsonForMatStr.charAt(jsonForMatStr.length() - 1)) {
                jsonForMatStr.append(getLevelStr(level));
            }
            switch (c) {
                case '{':
                case '[':
                    jsonForMatStr.append(c + "\n");
                    level++;
                    break;
                case ',':
                    jsonForMatStr.append(c + "\n");
                    break;
                case '}':
                case ']':
                    jsonForMatStr.append("\n");
                    level--;
                    jsonForMatStr.append(getLevelStr(level));
                    jsonForMatStr.append(c);
                    break;
                default:
                    jsonForMatStr.append(c);
                    break;
            }
        }

        return jsonForMatStr.toString();

    }


    private static String getLevelStr(int level) {
        StringBuffer levelStr = new StringBuffer();
        for (int levelI = 0; levelI < level; levelI++) {
            levelStr.append("\t");
        }
        return levelStr.toString();
    }

}
