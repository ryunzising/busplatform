package com.westwood.busplatform.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class KMBETAdto {
    String type;
    String version;
    Date generated_timestamp;
    List<data> data;
    @Data
    public static class data{
        String co;
        String route;
        String dir;
        String service_type;
        String dest_tc;
        String dest_sc;
        String dest_en;
        String eta_seq;
        Date eta;
        String rmk_tc;
        String rmk_sc;
        String rmk_en;
        Date data_timestamp;
    }
}
