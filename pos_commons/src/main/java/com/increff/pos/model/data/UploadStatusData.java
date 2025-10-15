package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UploadStatusData {

    private Integer totalCount;
    private Integer successCount;
    private Integer errorCount;
    private List<String> errorMessages;

}
