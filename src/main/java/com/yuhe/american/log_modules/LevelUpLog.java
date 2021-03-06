package com.yuhe.american.log_modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.yuhe.american.db.log.CommonDB;
import com.yuhe.american.db.statics.UserInfoDB;
import com.yuhe.american.utils.RegUtils;

import net.sf.json.JSONObject;

public class LevelUpLog extends AbstractLogModule {

	private static final String[] LOG_COLS = { "Uid", "Type", "OrgLevel", "NewLevel", "Exp", "Urs", "Name" };
	private static final String[] DB_COLS = { "HostID", "Uid", "Type", "OrgLevel", "NewLevel", "Exp", "Urs", "Name",
			"Time" };
	private static String TBL_NAME = "tblLevelUpLog";
	private static Map<String, String> TYPE_MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			put("LevelUp", "1");
			put("addplayer", "2");
		}
	};

	@Override
	public Map<String, List<Map<String, String>>> execute(List<String> logList, Map<String, String> hostMap) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		for (String logStr : logList) {
			JSONObject json = JSONObject.fromObject(logStr);
			if (json != null) {
				String message = json.getString("message");
				String hostID = json.getString("hostid");
				if (StringUtils.isNotBlank(message) && hostMap.containsKey(hostID)) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("HostID", hostID);
					String time = RegUtils.getLogTime(message);
					map.put("Time", time);
					for (String col : LOG_COLS) {
						String value = RegUtils.getLogValue(message, col, "");
						if (col.equals("Type")) { // 升级类型需要类型转换
							value = TYPE_MAP.getOrDefault(value, "1");
						}
						map.put(col, value);
					}
					String platformID = hostMap.get(hostID);
					List<Map<String, String>> platformResult = platformResults.get(platformID);
					if (platformResult == null)
						platformResult = new ArrayList<Map<String, String>>();
					if (StringUtils.isNotBlank(map.get("Uid"))) { // uid不为空的才添加
						platformResult.add(map);
						platformResults.put(platformID, platformResult);
					}
				}
			}
		}
		// 插入数据库
		Iterator<String> it = platformResults.keySet().iterator();
		while (it.hasNext()) {
			String platformID = it.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			CommonDB.batchInsertByDate(platformID, platformResult, DB_COLS, TBL_NAME);
			UserInfoDB.batchUpdateLevel(platformID, platformResult);
		}
		return platformResults;
	}

	@Override
	public String getStaticsIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<Map<String, String>>> execute4Kafka(JSONObject json, Map<String, String> staticsHosts) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		String message = json.getString("message");
		String hostID = json.getString("hostid");
		if (StringUtils.isNotBlank(message) && staticsHosts.containsKey(hostID)) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("HostID", hostID);
			String time = RegUtils.getLogTime(message);
			map.put("Time", time);
			for (String col : LOG_COLS) {
				String value = RegUtils.getLogValue(message, col, "");
				if (col.equals("Type")) { // 升级类型需要类型转换
					value = TYPE_MAP.getOrDefault(value, "1");
				}
				map.put(col, value);
			}
			String platformID = staticsHosts.get(hostID);
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			if (platformResult == null)
				platformResult = new ArrayList<Map<String, String>>();
			platformResult.add(map);
			platformResults.put(platformID, platformResult);
		}
		// 插入数据库
		Iterator<String> it = platformResults.keySet().iterator();
		while (it.hasNext()) {
			String platformID = it.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			CommonDB.batchInsertByDate(platformID, platformResult, DB_COLS, TBL_NAME);
			UserInfoDB.batchUpdateLevel(platformID, platformResult);
		}
		return platformResults;
	}

}
