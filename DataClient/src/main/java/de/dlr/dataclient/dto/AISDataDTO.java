package de.dlr.dataclient.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AISDataDTO {
    private String callsign;
    private String mmsi;
    private String position;
    private String heading;
    private double sog;
    private double cog;
    private double rot;
    private double length;
    private double width;
    private String draught;
    private String eta;
    private String theme;
    private String shiptype;
    private String imo;
    private String timestamp;
    private String destination;
    private long id;
    private String radarid;
    private String referenceid;
    private String name;
    private String navstatus;
    private String bow;
    private String stern;
    private String port;
    private String starboard;
}
