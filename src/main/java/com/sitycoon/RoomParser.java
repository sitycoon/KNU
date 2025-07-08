package com.sitycoon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class RoomParser {

	public static Map<Integer, Integer> countRoomsByCapacity(String jsonString) {
		Map<Integer, Integer> roomCountMap = new HashMap<>();

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(jsonString);
			JsonNode rooms = root.get("result");

			if (rooms == null || !rooms.isArray() || rooms.size() == 0) {
				// System.out.println("객실 데이터가 없습니다.");
				return roomCountMap; // 빈 맵 반환
			}

			for (JsonNode room : rooms) {
				int maxCapa = room.get("maxCapa").asInt();
				roomCountMap.put(maxCapa, roomCountMap.getOrDefault(maxCapa, 0) + 1);
			}

		} catch (Exception e) {
			System.out.println("JSON 파싱 오류: " + e.getMessage());
		}

		return roomCountMap;
	}
}
