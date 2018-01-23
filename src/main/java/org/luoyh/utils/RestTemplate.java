package com.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.Maps;

/**
 * @author luoyh
 * @date Jun 12, 2018
 */
@Controller
public class BaseController {

	@Value("${api.core.bluewhale}")
	protected String coreUrl;

	@Value("${api.serve.url}")
	protected String serveUrl;
	
	@Value("${api.static.resource.url}")
	private String staticResourceUrl;
	
	/**
	 * 互动广告链接地址
	 */
	@Value("${advertisement.server.link.url}")
	protected String advertisementUrl;
	
	private static Logger log = LoggerFactory.getLogger(BaseController.class);
	
	protected static Cache cache = Cache.INSTANCE;


	@ModelAttribute
	public void initBinder(ModelMap map) {
		map.put("resourceUrl", staticResourceUrl);
	}
	
	@Autowired
	private RestTemplate restClient;
	private static HttpHeaders textHeader = new HttpHeaders(); 
	private static HttpHeaders multipartHeader = new HttpHeaders();  
	private static HttpHeaders formHeader = new HttpHeaders();
	private static HttpHeaders jsonHeader = new HttpHeaders();
	
	static {
		textHeader.setContentType(new MediaType("text" ,"plain", Charset.forName(Cons.CHAR_SET_NAME)));  
		multipartHeader.setContentType(MediaType.MULTIPART_FORM_DATA);  
		formHeader.setContentType(new MediaType("application" ,"x-www-form-urlencoded", Charset.forName("UTF-8")));
		jsonHeader.setContentType(new MediaType("application" ,"json", Charset.forName("UTF-8")));
	}
	
	
	
	private <T> ResponseEntity<T> finalResponseEntity(ResponseEntity<T> entity, String url) {
		if(entity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
			log.error("request {} 500 {}", url, entity.getBody());
		}
		log.info("request {} {}", url, entity.getStatusCode().toString());
		return new ResponseEntity<T>(entity.getBody(), entity.getStatusCode());
	}
	
	public <T, V> ResponseEntity<T> postForEntity(String url, Class<T> clazz, MultiValueMap<String, V> param) {
		log.info("Post: {}, {}", url, param);
		MultiValueMap<String, String> finalParam = objectToString(param);
		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(finalParam, formHeader);
		return finalResponseEntity(restClient.postForEntity(url, requestEntity, clazz), url);
	}
	
	public <T, V> ResponseEntity<T> postForEntityJson(String url, Class<T> clazz, MultiValueMap<String, V> param) {
		log.info("Post: {}, {}", url, param);
		HttpEntity<String> requestEntity = new HttpEntity<String>(JsonUtils.writeObjToString(param.toSingleValueMap()), jsonHeader);
		return finalResponseEntity(restClient.postForEntity(url, requestEntity, clazz), url);
	}

	public <T, V> ResponseEntity<T> postForEntityJson(String url, Class<T> clazz, String param) {
		log.info("Post: {}, {}", url, param);
		HttpEntity<String> requestEntity = new HttpEntity<String>(param, jsonHeader);
		return finalResponseEntity(restClient.postForEntity(url, requestEntity, clazz), url);
	}
	
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> clazz, MultiValueMap<String, String> param) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParams(param);
		URI uri = builder.build().encode().toUri();
		log.info("Get: {}", uri);
		return finalResponseEntity(restClient.getForEntity(uri, clazz), url);
	}
	
	public <T> ResponseEntity<T> postFile(String url, Class<T> clazz, MultiValueMap<String, Object> param) {
		log.info("Post File: {}, {}", url, param);
		MultiValueMap<String, Object> finalParam = newMultiValueMap();
		if(null != param) {
			for (String key : param.keySet()) {
				for (Object value : param.get(key)) {
					if(null != value && value instanceof String) {
						HttpEntity<String> entity = new HttpEntity<String>(value.toString(), textHeader);
						finalParam.add(key, entity);
					} else {
						finalParam.add(key, value);
					}
				}
			}
		}
		
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<MultiValueMap<String, Object>>(finalParam, multipartHeader);  
		
		return finalResponseEntity(restClient.postForEntity(url, requestEntity, clazz), url);
	}

	public <V> ResponseEntity<ApiResult> postForApiResult(String url, MultiValueMap<String, V> param) {
		return postForEntity(url, ApiResult.class, param);
	}
	
	public ResponseEntity<ApiResult> getForApiResult(String url, MultiValueMap<String, String> param) {
		return getForEntity(url, ApiResult.class, param);
	}
	
	@SuppressWarnings("rawtypes")
	public <E, M> ResponseEntity<Map> getForMap(String url, MultiValueMap<String, String> param) {
		return getForEntity(url, Map.class, param);
	}
	
	public <T> ResponseEntity<T> newResponseEntity(T t, HttpStatus code) {
		return new ResponseEntity<T>(t, code);
	}

	public <T> ResponseEntity<T> newOkResponseEntity(T t) {
		return newResponseEntity(t, HttpStatus.OK);
	}
	
	public <V> MultiValueMap<String, String> objectToString(MultiValueMap<String, V> param) {
		MultiValueMap<String, String> finalParam = newMultiValueMap();
		if(null != param) {
			for (String key : param.keySet()) {
				for (Object value : param.get(key)) {
					if(null != value) {
						finalParam.add(key, value.toString());
					}
				}
			}
		}
		return finalParam;
	}
	
	public MultiValueMap<Object, Object> buildMultiValueMap(Object ... objects) {
		if(null == objects || objects.length == 0) {
			return null;
		}
		MultiValueMap<Object, Object> param = new LinkedMultiValueMap<Object, Object>();
		for(int i = 0, len = objects.length; i < len; i ++) {
			if(i + 1 >= len) break;
			param.add(objects[i], objects[++ i]);
		}
		return param;
	}

	public <K, V> MultiValueMap<K, V> newMultiValueMap() {
		return new LinkedMultiValueMap<K, V>();
	}
	
	protected String encodeURI(String s) {
		try {
			return URLEncoder.encode(s, Cons.CHAR_SET_NAME);
		} catch (UnsupportedEncodingException e) {
		}
		return "";
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void _EA(
			String link,
			MultiValueMap<String, Object> param,
			RequestMethod method,
			String filename, 
			String wraps, // FIX : (\\w+)(\\[(\\d+)\\])?
			String[] head,
			String[] body,
			Map<String, String> bodyWrap,
			Map<String, Map<String, String>> dist,
			HttpServletResponse response) {
		if(StringUtils.isBlank(link) 
				|| StringUtils.isBlank(wraps) 
				|| null == response 
				|| null == head
				|| head.length == 0
				|| null == body
				|| body.length == 0
				|| head.length != body.length) {
			return;
		}
		ResponseEntity<Map> r = null;
		if(null == method || method != RequestMethod.POST) {
			r = getForEntity(link, Map.class, objectToString(param));
		} else {
			r = postForEntity(link, Map.class, param);
		}
		if(null == r || null == r.getBody()) {
			try (PrintWriter out = response.getWriter()) {
				out.write("导出异常");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		if(r.getStatusCode() != HttpStatus.OK) {
			try (PrintWriter out = response.getWriter()) {
				out.write("导出异常  [" + r.getStatusCode() + "]");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		if(StringUtils.isBlank(filename)) {
			filename = UuidUtils.millis62String();
		}
	
		response.setContentType("application/vnd.openxmlformats;charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\""+encodeURI(filename)+".xls\"");
		try (OutputStream out = response.getOutputStream()) {
			StringBuilder sh = new StringBuilder();
			sh.append("<style>td{mso-number-format:\"\\@\";}</style><table border=\"1\"><thead><tr>");
			for(String h : head) {
				sh.append("<th>").append(h).append("</th>");
			}
			sh.append("</tr></thead><tbody>");
			out.write(sh.toString().getBytes(Cons.CHAR_SET_NAME));
			Map<String, Object> map = r.getBody();
			if(null == map || map.isEmpty()) {
				out.write("</tbody>".getBytes());
				out.flush();
				return;
			}
			String[] wrap = wraps.split("\\.");
			for(int i = 0, len = wrap.length; i < len; i ++) {
				String key = wrap[i];
				if(!StringUtils.isBlank(key)) {
					Object obj = map.get(key);
					if(null == obj) {
						break;
					}
					if(i == len - 1) {
						if(Iterable.class.isAssignableFrom(obj.getClass())) {
							Iterable<Map> iter = (Iterable<Map>) obj;
							for(Map e : iter) {
								StringBuilder sb = new StringBuilder();
								sb.append("<tr>");
								for(String td : body) {
									sb.append("<td>").append(val(td, e, bodyWrap, dist)).append("</td>");
								}
								sb.append("</tr>");
								out.write(sb.toString().getBytes(Cons.CHAR_SET_NAME));
							}
						} else {
							break;
						}
					} else {
						map = (Map<String, Object>) obj;
					}
				}
			}
			
			out.write("</tbody>".getBytes());
			
			out.flush();
		} catch(IOException ex) { 
			ex.printStackTrace();
		}
		
		
	}
	private String val(String td, Map<?, ?> e, Map<String, String> wrap, Map<String, Map<String, String>> dist) {
		String v = "";
		try {
			boolean find = false;
			if(null != wrap && !wrap.isEmpty()) {
				String tdWrap = wrap.get(td);
				if(!StringUtils.isBlank(tdWrap)) {
					find = true;
					String[] ws = tdWrap.split("\\.");
					Map<?, ?> ee = Maps.newHashMap(e);
					Object fe = null;
					for(int i = 0, len = ws.length; i < len; i ++) {
						Object _e = ee.get(ws[i]);
						if(null == _e) {
							return "";
						}
						if(i == len - 1) {
							fe = _e;
						} else {
							ee = (Map<?, ?>) _e;
						}
					}
					if(null != fe) {
						v = fe.toString();
					}
				}
			}
			if(!find) {
				v = val(e.get(td));
			}
			if(null != dist) {
				Map<String, String> d = dist.get(td);
				if(null != d) {
					String fv = d.get(v);
					if(null != fv) {
						return fv;
					}
				}
			}
		} catch(Exception ex) {
			log.error(ex.getMessage());
			v = "";
		}
		return v;
	}
	private String val(Object v) {
		return null == v ? "" : v.toString();
	}
}
