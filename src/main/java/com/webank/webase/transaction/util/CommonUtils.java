/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.webank.webase.transaction.util;

import com.webank.webase.transaction.base.BasePageRsp;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ResponseEntity;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.utils.Numeric;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * CommonUtils.
 *
 */
@Slf4j
public class CommonUtils {

    public static final int publicKeyLength_64 = 64;

    /**
     * base response
     */
    public static ResponseEntity buildSuccessRsp(Object data) {
        ResponseEntity baseRspVo = new ResponseEntity(ConstantCode.RET_SUCCEED);
        baseRspVo.setData(data);
        return baseRspVo;
    }
    /**
     * base page response
     */
    public static ResponseEntity buildSuccessPageRspVo(Object data, long totalCount) {
        BasePageRsp basePageRspVo = new BasePageRsp(ConstantCode.RET_SUCCEED);
        basePageRspVo.setData(data);
        basePageRspVo.setTotalCount(totalCount);
        return basePageRspVo;
    }
    /**
     * buildHeaders.
     *
     * @return
     */
    public static HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return headers;
    }

    /**
     * stringToSignatureData.
     * byte array: [v + r + s + pub]
     * 19/12/24 support guomi： add byte[] pub in signatureData
     * 2021/08/05 webase-sign <=1.4.3, v=27 >=1.5.0, v=0 or 1 or 2
     * if using web3sdk, v default 27, if using java-sdk, v default 0, and add 27 in RLP encode
     *
     * @param signatureData signatureData
     * @return
     */
    public static SignatureData stringToSignatureData(String signatureData, int encryptType) {
        byte[] byteArr = Numeric.hexStringToByteArray(signatureData);
        // java-sdk的v值与web3sdk不一样，front>1.5.3
        byte signVOfJavaSdk = byteArr[0];
        byte signVOfWeb3Sdk = (byte) ((int) signVOfJavaSdk + 27);
        byte[] signR = new byte[32];
        System.arraycopy(byteArr, 1, signR, 0, signR.length);
        byte[] signS = new byte[32];
        System.arraycopy(byteArr, 1 + signR.length, signS, 0, signS.length);
        if (encryptType == 1) {
            byte[] pub = new byte[64];
            System.arraycopy(byteArr, 1 + signR.length + signS.length, pub, 0, pub.length);
            return new SignatureData(signVOfWeb3Sdk, signR, signS, pub);
        } else {
            return new SignatureData(signVOfWeb3Sdk, signR, signS);
        }
    }

    /**
     * signatureDataToString.
     * 19/12/24 support guomi： add byte[] pub in signatureData
     * @param signatureData signatureData
     */
    public static String signatureDataToString(SignatureData signatureData, int encryptType) {
        byte[] byteArr;
        if(encryptType == 1) {
            byteArr = new byte[1 + signatureData.getR().length + signatureData.getS().length + 64];
            byteArr[0] = signatureData.getV();
            System.arraycopy(signatureData.getR(), 0, byteArr, 1, signatureData.getR().length);
            System.arraycopy(signatureData.getS(), 0, byteArr, signatureData.getR().length + 1,
                    signatureData.getS().length);
            System.arraycopy(signatureData.getPub(), 0, byteArr,
                    signatureData.getS().length + signatureData.getR().length + 1,
                    signatureData.getPub().length);
        } else {
            byteArr = new byte[1 + signatureData.getR().length + signatureData.getS().length];
            byteArr[0] = signatureData.getV();
            System.arraycopy(signatureData.getR(), 0, byteArr, 1, signatureData.getR().length);
            System.arraycopy(signatureData.getS(), 0, byteArr, signatureData.getR().length + 1,
                    signatureData.getS().length);
        }
        return Numeric.toHexString(byteArr, 0, byteArr.length, false);
    }

    /**
     * Object to JavaBean.
     *
     * @param obj obj
     * @param clazz clazz
     * @return
     */
    public static <T> T object2JavaBean(Object obj, Class<T> clazz) {
        if (obj == null || clazz == null) {
            log.warn("Object2JavaBean. obj or clazz null");
            return null;
        }
        String jsonStr = JsonUtils.toJSONString(obj);
        return JsonUtils.toJavaObject(jsonStr, clazz);
    }

    /**
     * delete single File.
     *
     * @param filePath filePath
     * @return
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * delete Files.
     *
     * @param path path
     * @return
     */
    public static boolean deleteFiles(String path) {
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File dirFile = new File(path);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        File[] files = dirFile.listFiles();
        if (files == null) {
            return false;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            } else {
                flag = deleteFiles(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            return false;
        }
        return true;
    }

    /**
     * 0 < signUserId <= 64
     * @param input
     */
    public static boolean checkLengthWithin_64(String input) {
        if (input.isEmpty() || input.length() > publicKeyLength_64) {
            return false;
        }
        return true;
    }
}
