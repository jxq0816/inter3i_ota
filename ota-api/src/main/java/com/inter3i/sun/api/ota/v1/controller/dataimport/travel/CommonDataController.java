/*
 *
 * Copyright (c) 2016, inter3i.com. All rights reserved.
 * All rights reserved.
 *
 * Author: Administrator
 * Created: 2016/12/12
 * Description:
 *
 */

package com.inter3i.sun.api.ota.v1.controller.dataimport.travel;

import com.inter3i.sun.api.ota.v1.config.ImportDataConfig;
import com.inter3i.sun.api.ota.v1.config.MongoDBServerConfig;
import com.inter3i.sun.api.ota.v1.service.ServiceFactory;
import com.inter3i.sun.api.ota.v1.service.dataimport.ICommonDataService;
import com.inter3i.sun.api.ota.v1.util.TimeStatisticUtil;
import com.inter3i.sun.api.ota.v1.util.ValidateUtils;
import com.inter3i.sun.persistence.NonSupportException;
import com.inter3i.sun.persistence.dataimport.CommonData;
import org.bson.Document;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController("/DocCache")
@RequestMapping("/DocCache")
public class CommonDataController {
    //cachedoc?type=insert http://wangcc:8080/DocCache/cachedoc?type=insert&commit=false&serverHost=wangcc:8071&cacheServerName=cache01

    private static final Logger logger = LoggerFactory.getLogger(CommonDataController.class);

    private static final String OPERATE_TYPE_INSERT = "insert";

    private final ICommonDataService commonDataService;
    //需要校验非空的字段
    final static String CHECK_FIELDS[] = new String[]{"column", "column1", "page_url", "original_url", "floor"};
   /* @Autowired
    private MongoDBServerConfig serverConfig;*/

    private MongoDBServerConfig serverConfig = MongoDBServerConfig.getConfig("dataSource1");


    CommonDataController() {
        commonDataService = ServiceFactory.commonDataService();
    }

    @RequestMapping("/timestatistic")
    public
    @ResponseBody
    String health() {
        //return "Green!";
        TimeStatisticUtil.TimeInfo timeInfo = TimeStatisticUtil.getTimeInof(serverConfig.getDataImportUrl(ImportDataConfig.CACHE_NAME_01));
        return timeInfo.toString();
    }

    @RequestMapping("/clearn")
    public void clearn() {
        //return "Green!";
        TimeStatisticUtil.removeByKey(serverConfig.getDataImportUrl(ImportDataConfig.CACHE_NAME_01));
    }

    /*@RequestMapping(value = "/cachedoc", method = {RequestMethod.GET, RequestMethod.POST})
    public
    @ResponseBody
    ResponseBean saveDocs(@RequestParam("type") String types, @RequestBody CommonData.CommonDatas commonDatas) {
        ResponseBean responseData = new ResponseBean();
        try {
            if (OPERATE_TYPE_INSERT.equals(types)) {
                commonDataService.savaCommonData(commonDatas);
            } else {
                throw new NonSupportException("unsupported type:[" + types + "] for controller:[CommonDataController]");
            }
            logger.info("Import common document success.");
        } catch (Exception e) {
            logger.error("Import common document exception:[" + e.getMessage() + "].", e);
            responseData.setSuccess(false);
            responseData.setErrorMsg("Import common document exception:[" + e.getMessage() + "].");
        } finally {
            return responseData;
        }
    }*/

    //@RequestMapping(produces = "application/json;charset=UTF-8", value = "/cachedoc", method = RequestMethod.POST)
    //@RequestMapping(produces = "text/xml;charset=utf8", value = "/cachedoc", method = {RequestMethod.GET, RequestMethod.POST}) /{cacheServerName}/{type}
    @RequestMapping(produces = "text/xml;charset=utf8", value = "/cachedoc", method = {RequestMethod.POST})
    public
    @ResponseBody
    //String saveDocs(@PathVariable String cacheServerName, @PathVariable String type, @RequestBody String requestDataStr) {
    String saveDocs(@RequestParam("type") String type, @RequestParam("cacheServerName") String cacheServerName, @RequestBody String requestDataStr) {
//        private boolean success = true;
//        private String errorCode;
//        private String errorMsg;
        JSONObject responseData = new JSONObject();
        try {
            responseData.put("success", true);
            logger.debug("Import common document for Cache:" + cacheServerName + "] requestDoc: " + requestDataStr);
            //校验当前的缓存是否合法 PHP端不能随意指定cacheName,指定的cacheName必须在配置文件中进行配置
            serverConfig.validateCacheName(cacheServerName);

            Document doc = Document.parse(requestDataStr);
            ArrayList DocDatas = (ArrayList) doc.get("datas");
            //非空校验
            for (int i1 = 0; i1 < DocDatas.size(); i1++) {
                Document DocData = (Document) DocDatas.get(i1);
                for (int i = 0; i < CHECK_FIELDS.length; i++) {
                    if (ValidateUtils.isNullOrEmpt(DocData.get(CHECK_FIELDS[i]))) {
                        logger.error("Import document exception: the field [ " + CHECK_FIELDS[i] + " ] is null.");
                        throw new RuntimeException(" Import document exception: the field [" + CHECK_FIELDS[i] + " ] is null.");
                    }
                }
            }

            CommonData commonData = new CommonData();
            commonData.setImportStatus(CommonData.IMPORTSTATUS_NO_IMPORT);
            commonData.setSegmentedStatus(CommonData.SEGMENTE_SATUS_NO);
            commonData.setCacheDataTime(System.currentTimeMillis());
            commonData.setJsonDoc(doc);

            if (OPERATE_TYPE_INSERT.equals(type)) {
                commonDataService.savaCommonData(cacheServerName, commonData, serverConfig);
            } else {
                throw new NonSupportException("unsupported type:[" + type + "] for controller:[CommonDataController]");
            }
            logger.info("Import common document success.");
        } catch (Exception e) {
            logger.error("Import common document exception:[" + e.getMessage() + "].", e);
            responseData.put("success", false);
            responseData.put("errorMsg", "Import common document exception:[" + e.getMessage() + "].");
        } finally {
            return responseData.toString();
        }
    }

}
