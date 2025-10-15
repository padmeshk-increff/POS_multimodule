package com.increff.pos.utils;

import com.increff.pos.model.data.UploadStatusData;

import java.util.List;

public class BaseUtil {
    public static UploadStatusData convert(Integer totalRows, List<String> errors){
        UploadStatusData uploadStatusData = new UploadStatusData();
        uploadStatusData.setTotalCount(totalRows);
        uploadStatusData.setErrorCount(errors.size());
        uploadStatusData.setSuccessCount(totalRows - errors.size());
        uploadStatusData.setErrorMessages(errors);
        return uploadStatusData;
    }
}
