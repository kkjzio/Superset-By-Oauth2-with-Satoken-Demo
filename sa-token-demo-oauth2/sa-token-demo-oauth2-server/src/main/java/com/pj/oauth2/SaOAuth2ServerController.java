package com.pj.oauth2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import com.bazaarvoice.jackson.rison.RisonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.pj.client.Client;
import lombok.Data;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.oauth2.config.SaOAuth2Config;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Handle;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;


/**
 * Sa-OAuth2 Server端 控制器
 *
 * @author click33
 */
@RestController
public class SaOAuth2ServerController {

    @Autowired
    private Client client;

    //    private static final String SUPERSET_HOST = "http://192.168.59.129:8088";
    private final String SUPERSET_FILTER_URL_L = "/superset/dashboard/";
    private final String SUPERSET_FILTER_URL_R = "/?standalone=3&show_filters=0&native_filters=";
//	private static final Integer SUPERSET_PORT = 8088;
//	private static final String LIST_DASHBOARD_API_ENDPOINT = "/api/v1/dashboard/";

    // 处理所有OAuth相关请求
    @RequestMapping("/oauth2/*")
    public Object request() {
        System.out.println("------- 进入请求: " + SaHolder.getRequest().getUrl());
        System.out.println("------- 报文为: ");
        SaHolder.getRequest().getParamMap().forEach((k, v) -> {
            System.out.println("Key: " + k + ", Value: " + v);
        });
        Object o = SaOAuth2Handle.serverRequest();
        // token的时候，返回的报文中增加access_token
        if (o instanceof SaResult) {
            SaResult o2 = (SaResult) o;
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("code", o2.getCode());
            map.put("msg", o2.getMsg());
            map.put("data", o2.getData());
            Object data = o2.getData();
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                if (Objects.nonNull(dataMap.get("access_token"))) {
                    map.put("access_token", dataMap.get("access_token"));
                }
            }
            System.out.println("------- 返回报文为: ");
            map.forEach((k, v) -> {
                System.out.println("Key: " + k + ", Value: " + v);
            });
            return map;
        }
        return o;
    }

    // Sa-OAuth2 定制化配置
    @Autowired
    public void setSaOAuth2Config(SaOAuth2Config cfg) {
        cfg.
                // 未登录的视图
                        setNotLoginView(() -> {
                    return new ModelAndView("login.html");
                }).
                // 登录处理函数
                        setDoLoginHandle((name, pwd) -> {
                    // 管理员测试账户
                    if ("sa".equals(name) && "123456".equals(pwd)) {
                        StpUtil.login(10001);
                        return SaResult.ok();
                    }
                    // 普通用户测试账户
                    if ("user".equals(name) && "123456".equals(pwd)) {
                        StpUtil.login(10002);
                        return SaResult.ok();
                    }
                    return SaResult.error("账号名或密码错误");
                }).
                // 授权确认视图
                        setConfirmView((clientId, scope) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("clientId", clientId);
                    map.put("scope", scope);
                    return new ModelAndView("confirm.html", map);
                })
        ;
    }

    // 全局异常拦截
    @ExceptionHandler
    public SaResult handlerException(Exception e) {
        e.printStackTrace();
        return SaResult.error(e.getMessage());
    }


    // ---------- 开放相关资源接口： Client端根据 Access-Token ，置换相关资源 ------------

    // 获取Userinfo信息：昵称、头像、性别等等
    @RequestMapping("/oauth2/userinfo")
    public SaResult userinfo() {
        // 获取 Access-Token 对应的账号id
        System.out.println("------- 进入请求: " + SaHolder.getRequest().getUrl());
        System.out.println("------- 报文为: ");
        SaHolder.getRequest().getParamMap().forEach((k, v) -> {
            System.out.println("Key: " + k + ", Value: " + v);
        });
        String accessToken = SaHolder.getRequest().getParam("access_token");
        // 若是从请求参数中找不到access_token，就去请求头里找
        if (Objects.isNull(accessToken)) {
            accessToken = SaHolder.getRequest().getHeader("Authorization");
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
        }
        Object loginId = SaOAuth2Util.getLoginIdByAccessToken(accessToken);
        System.out.println("-------- 此Access-Token对应的账号id: " + loginId);

        // 校验 Access-Token 是否具有权限: userinfo
        SaOAuth2Util.checkScope(accessToken, "userinfo");

        // 模拟账号信息 （真实环境需要查询数据库获取信息）
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (String.valueOf(loginId).equals("10001")) {
            map.put("username", "admin");
            map.put("nickname", "shengzhang_");
            map.put("avatar", "http://xxx.com/1.jpg");
            map.put("age", "18");
            map.put("sex", "男");
            map.put("address", "山东省 青岛市 城阳区");
            map.put("email", "admin@zxc.com");
            map.put("first_name", "Admin");
            map.put("last_name", "Admin");
        } else {
            map.put("username", "userKkjz");
            map.put("nickname", "kkjz");
            map.put("avatar", "http://xxx.com/1.jpg");
            map.put("age", "18");
            map.put("sex", "男");
            map.put("address", "山东省 青岛市 城阳区");
            map.put("email", "kkjz@zxc.com");
            map.put("first_name", "Kkjz");
            map.put("last_name", "kkjz");
        }

        return SaResult.data(map);
    }

    @RequestMapping("/oauth2/superset/videoSearch")
    public SaResult videoSearch() throws IOException {
        System.out.println("------- 进入请求: " + SaHolder.getRequest().getUrl());
        System.out.println("------- 报文为: ");
        SaHolder.getRequest().getParamMap().forEach((k, v) -> {
            System.out.println("Key: " + k + ", Value: " + v);
        });
        Map<String, String> paramMap = SaHolder.getRequest().getParamMap();

        // 登录处理
        String accessToken = SaHolder.getRequest().getParam("access_token");
        // 若是从请求参数中找不到access_token，就去请求头里找
        if (Objects.isNull(accessToken)) {
            accessToken = SaHolder.getRequest().getHeader("Authorization");
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
        }
        Object loginId = SaOAuth2Util.getLoginIdByAccessToken(accessToken);
        System.out.println("-------- 此Access-Token对应的账号id: " + loginId);
        // 校验 Access-Token 是否具有权限: userinfo
        SaOAuth2Util.checkScope(accessToken, "userinfo");

        JsonElement dashboards;
        try {
            dashboards = client.dashboards();
//			return SaResult.data(dashboards.getAsJsonArray());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (Objects.isNull(dashboards)) {
            return SaResult.error("获取数据失败");
        }

        // 处理json数据
        // 查看是否有请求的dashboard
        Gson gson = new Gson();
        JsonArray result = dashboards.getAsJsonObject().getAsJsonArray("result");
        JsonObject bashboardMeta = null;
        String slug = paramMap.get("slug");
        for (JsonElement res : result) {
            JsonObject resObj = res.getAsJsonObject();
            if (resObj.get("slug").getAsString().equals(slug)) {
                bashboardMeta = gson.fromJson(resObj.get("json_metadata").getAsString(), JsonObject.class);
            }
        }

        // 提取出筛选器的id、name和filterType
        Map<String, DashboardFilter> filterMap = new HashMap<>();

        // TODO：参数有效性校检
        if (Objects.nonNull(bashboardMeta)) {
            bashboardMeta.get("native_filter_configuration").getAsJsonArray().forEach(filter -> {
                JsonObject filterObj = filter.getAsJsonObject();
                DashboardFilter dashboardFilter = new DashboardFilter();
                dashboardFilter.setId(filterObj.get("id").getAsString());
                dashboardFilter.setName(filterObj.get("name").getAsString());
                dashboardFilter.setColumnName(removeOuterQuotes(filterObj.get("targets").getAsJsonArray().get(0)
                        .getAsJsonObject().get("column").getAsJsonObject().get("name").toString()));
                dashboardFilter.setFilterType(filterObj.get("filterType").getAsString());


                filterMap.put(dashboardFilter.getName(), dashboardFilter);
            });
        } else {
            return SaResult.error("未找到对应的dashboard");
        }

        // 与传过来的参数对比,将匹配成功的筛选器加入list中
        List<DashboardFilter> filterList = new ArrayList<>();
        JsonArray filterReq = gson.fromJson(SaHolder.getRequest().getParamMap().get("filters"), JsonArray.class);
        if (Objects.isNull(filterReq)) {
            return SaResult.error("筛选器为空");
        }
        for (JsonElement filter : filterReq) {
            JsonObject filterObj = filter.getAsJsonObject();
            DashboardFilter dashboardFilter = filterMap.get(filterObj.get("name").getAsString());
            //TODO:校检filterType类是否对应
            if (Objects.nonNull(dashboardFilter)) {
                // 数据装载
                // 有值才放，否则跳过
                if (Objects.isNull(filterObj.get("values"))) {
                    continue;
                }
//                String nums = generateNumsStr(removeOuterQuotes(filterObj.get("values").getAsString()));
                String nums = generateNumsStr(filterObj.get("values").getAsString());
                if (Objects.equals(nums, "")) {
                    continue;
                }
                dashboardFilter.setValues(nums);
                filterList.add(dashboardFilter);
            } else {
                return SaResult.error("筛选器" + filterObj.get("name") + "不存在");
            }
        }

        JsonObject jsonObject = generateNativeFiltersJson(filterList);
        // 将json变换为rison
        ObjectMapper RISON = new ObjectMapper(new RisonFactory());
        String risonStr = RISON.writeValueAsString(gson.fromJson(jsonObject, Map.class));
        HashMap<String, String> resultData = new HashMap<>();
        resultData.put("rison", risonStr);
        resultData.put("url", "http://" + client.getHost() + ":" + client.getPort() +
                SUPERSET_FILTER_URL_L + slug + SUPERSET_FILTER_URL_R + risonStr);

        return SaResult.data(resultData);

    }

    @RequestMapping("/oauth2/superset/videoSearchSql")
    public SaResult videoSearchSql(){
        System.out.println("------- 进入请求: " + SaHolder.getRequest().getUrl());
        System.out.println("------- 报文为: ");
        SaHolder.getRequest().getParamMap().forEach((k, v) -> {
            System.out.println("Key: " + k + ", Value: " + v);
        });
        Map<String, String> paramMap = SaHolder.getRequest().getParamMap();

        // 登录处理
        String accessToken = SaHolder.getRequest().getParam("access_token");
        // 若是从请求参数中找不到access_token，就去请求头里找
        if (Objects.isNull(accessToken)) {
            accessToken = SaHolder.getRequest().getHeader("Authorization");
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
        }
        Object loginId = SaOAuth2Util.getLoginIdByAccessToken(accessToken);
        System.out.println("-------- 此Access-Token对应的账号id: " + loginId);
        // 校验 Access-Token 是否具有权限: userinfo
        SaOAuth2Util.checkScope(accessToken, "userinfo");

//        JsonElement dashboards;

        // 处理json数据,生成sql语句
        Gson gson = new Gson();
        JsonObject searchValueElement = gson.fromJson(paramMap.get("searchValue"), JsonElement.class).getAsJsonObject();
        String searchValue1 = searchValueElement.get("value1").getAsString();
        String searchValue2 = searchValueElement.get("value2").getAsString();

        // sql字符串注入检测
        if (!isNumericOrHyphen(searchValue1) ||
                !isNumericOrHyphen(searchValue2) ||
                SqlInjectionUtils.check(searchValue1) ||
                SqlInjectionUtils.check(searchValue2)) {
            return SaResult.error("输入的搜索值不合法");
        }


        SQL sql = new SQL()
                .SELECT("videoID", "videoName", "videocAtegory","videoTime")
                .FROM("bilibili.video")
                .WHERE("'videoID' >= " + Integer.parseInt(searchValue1)+ " AND 'videoID' <= " + Integer.parseInt(searchValue2));
        String sqlText = sql.toString();


        



        return SaResult.data(sqlText);

    }


//
//	public static HttpUriRequest geListDashboardsRequest(String host, int port, String authToken)
//			throws URISyntaxException {
//		JsonArray columns = new JsonArray();
//		columns.add("dashboard_title");
//		columns.add("id");
//		JsonObject param = new JsonObject();
//		param.add("columns", columns);
//
//		URI apiUri = buildUri(host, port, LIST_DASHBOARD_API_ENDPOINT,
//				Arrays.asList(Pair.of("q", param.toString())));
//
//		HttpUriRequest get = RequestBuilder.get() //
//				.setUri(apiUri) //
//				.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken) //
//				.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
//				.build();
//		return get;
//	}
//
//	private static URI buildUri(String host, int port, String endpoint, List<Pair<String, String>> params)
//			throws URISyntaxException {
//		URIBuilder builder = new URIBuilder();
//		builder.setScheme("http").setHost(host).setPort(port).setPath(endpoint);
//
//		if (!CollectionUtils.isEmpty(params)) {
//			params.stream().forEach(p -> {
//				builder.addParameter(p.getKey(), p.getValue());
//			});
//		}
//
//		return builder.build();
//	}
//
//
//	private ApiResponse executeRequest(HttpUriRequest request)
//			throws ClientProtocolException, IOException, superset.client.exception.UnexceptedResponseException {
//		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
//			try (CloseableHttpResponse response = client.execute(request)) {
//				int code = response.getStatusLine().getStatusCode();
//				String bodyAsString = EntityUtils.toString(response.getEntity());
//				if (code >= 300 || code < 200) {
//					throw new superset.client.exception.UnexceptedResponseException(request.getURI().toString(), code, bodyAsString);
//				}
//				return new ApiResponse(code, bodyAsString);
//			}
//		}
//	}
//
//
////	public Client(String host, int port, String username, String password, CloseableHttpClient client)
////			throws Exception {
////		if (client == null) {
////			this.client = HttpClientBuilder.create().build();
////		} else {
////			this.client = client;
////		}
//////		HttpUriRequest request = Api.getAuthTokenRequest(host, port, username, password);
////		ApiResponse resp = executeRequest(request);
////		JsonElement respBody = JsonParser.parseString(resp.getBody());
////		String token = respBody.getAsJsonObject().get("access_token").getAsString();
////		this.authToken = token;
////		this.host = host;
////		this.port = port;
////	}
//
//	@AllArgsConstructor
//	@Data
//	public static class ApiResponse {
//		private int code;
//		private String body;
//	}

    private String removeOuterQuotes(String jsonString) {
        String regex = "^\"(.*)\"$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jsonString);
        // 判断是否匹配成功
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return jsonString;
        }

    }

    private String generateNumsStr(String input) {
//        String input = "1,2,4-6";
        List<String> result = new ArrayList<>();

        String[] parts = input.split(",");
        for (String part : parts) {
            if (!isNumericOrHyphen(part)) {
                System.out.println("Invalid input: " + part);
                return null;
            }

            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                for (int i = start; i <= end; i++) {
                    result.add(String.valueOf(i));
                }
            } else {
                result.add(part);
            }
        }

        return String.join(",", result);
    }

    private static boolean isNumericOrHyphen(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c) && c != '-') {
                return false;
            }
        }
        return true;
    }

    private JsonObject generateNativeFiltersJson(List<DashboardFilter> filterList) {
        JsonObject result = new JsonObject();
        Gson gson = new Gson();

        for (DashboardFilter filter : filterList) {
            JsonObject filterObject = new JsonObject();
            // 将字符串转换成数组
            List<String> nums = Arrays.asList(filter.getValues().split(","));

            JsonObject cacheObject = new JsonObject();
            cacheObject.add("label", gson.toJsonTree(nums));
            cacheObject.addProperty("validateStatus", false);
            cacheObject.add("value", gson.toJsonTree(nums));

            JsonObject extraFormDataObject = new JsonObject();
            JsonArray filtersArray = new JsonArray();
            JsonObject filtersObject = new JsonObject();
            filtersObject.addProperty("col", filter.getColumnName());
            filtersObject.addProperty("op", "IN");
            filtersObject.add("val", gson.toJsonTree(nums));
            filtersArray.add(filtersObject);
            extraFormDataObject.add("filters", filtersArray);

            filterObject.add("__cache", cacheObject);
            filterObject.add("extraFormData", extraFormDataObject);
            filterObject.add("filterState", cacheObject);
            filterObject.addProperty("id", filter.getId());
            filterObject.add("ownState", cacheObject);

            result.add(filter.getId(), filterObject);
        }

        return result;
    }

    @Data
    class DashboardFilter {
        private String id;
        private String name;
        private String filterType;
        private String columnName;
        private String values;
    }

    @Data
    class SqlSeacrch {
//        private String tableName;
        private String columns;
        private String op;
        private String value;
    }

}
