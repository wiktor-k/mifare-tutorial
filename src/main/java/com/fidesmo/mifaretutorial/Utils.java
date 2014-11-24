package com.fidesmo.mifaretutorial;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Static methods to be used in the example service provider
 */
public class Utils {

    // Returns JSON encoding of all parameters for one sector's trailer
    public static JSONObject encodeSectorTrailer(int numSector, String keyA, String keyB, String accessBits) {
        JSONObject keyPair = new JSONObject();
        keyPair.put ("keyA", keyA);
        keyPair.put ("keyB", keyB);
        JSONObject sector = new JSONObject();
        sector.put("sector", numSector);
        sector.put("keyPair", keyPair);
        sector.put("accessBits", accessBits);
        return sector;
    }

    // Returns JSON encoding of a block (inside a sector) to be read
    public static JSONObject encodeBlockToRead(int numSector, int numBlock) {
        JSONObject block = new JSONObject();
        block.put ("sector", numSector);
        block.put ("block", numBlock);
        return block;
    }

    // Decodes a counter from a JSONObject containing a list of blocks
    public static long decodeCounter(JSONArray blocks) {
        JSONObject block = (JSONObject) blocks.get(0);
        String hexValue = (String) block.get("data");
        return Long.parseLong(hexValue, 16);
    }

    // Encodes an integer into a "block" JSON object
    public static JSONObject encodeIntInBlock(long value, int numSector, int numBlock) {
        JSONObject block = new JSONObject();
        block.put ("sector", numSector);
        block.put ("block", numBlock);
        block.put ("data", String.format("%32h", value).replace(' ', '0'));
        return block;
    }

}
