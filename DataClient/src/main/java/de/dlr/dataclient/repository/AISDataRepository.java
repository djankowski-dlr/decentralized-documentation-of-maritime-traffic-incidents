package de.dlr.dataclient.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.dlr.dataclient.connection.PostgresConnection;
import de.dlr.dataclient.dto.AISDataDTO;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Log4j2
@Repository
public final class AISDataRepository {

    private final PostgresConnection postgresConnection;

    @Autowired
    public AISDataRepository(PostgresConnection postgresConnection) {
        this.postgresConnection = postgresConnection;
    }

    public List<AISDataDTO> getData(int first, long offset) {
        try {
            final Connection connection = this.postgresConnection.getConnection();
            final String query = queryBuilder(first, offset);
            final PreparedStatement stm = connection.prepareStatement(query);
            final ResultSet resultset = stm.executeQuery();
            final List<AISDataDTO> aisDataList = parseResultSet(resultset);
            log.debug("Data has been received by the database");
            return aisDataList;
        } catch (SQLException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    public String queryBuilder(long first, long offset) {
        String query = "SELECT * FROM TRACKS";

        if (offset != 0) {
            final String tmp = " WHERE id>= " + offset;
            query = query + tmp;
        }

        if (first != 0) {
            final String tmp = " LIMIT " + first;
            query = query + tmp;
        }

        return query + ";";
    }

    @SneakyThrows
    private static List<AISDataDTO> parseResultSet(ResultSet resultSet) {
        final List<AISDataDTO> aisDynamicMessages = new ArrayList<>();
        while (resultSet.next()) {
            final AISDataDTO aisDataDTO = new AISDataDTO();
            aisDataDTO.setId(resultSet.getLong("id"));
            aisDataDTO.setReferenceid(resultSet.getString("referenceid"));
            aisDataDTO.setMmsi(resultSet.getString("mmsi"));
            aisDataDTO.setPosition(resultSet.getString("position"));
            aisDataDTO.setHeading(resultSet.getString("heading"));
            aisDataDTO.setSog(resultSet.getDouble("sog"));
            aisDataDTO.setCog(resultSet.getDouble("cog"));
            aisDataDTO.setRot(resultSet.getDouble("rot"));
            aisDataDTO.setCallsign(resultSet.getString("callsign"));
            aisDataDTO.setLength(resultSet.getDouble("length"));
            aisDataDTO.setWidth(resultSet.getDouble("width"));
            aisDataDTO.setDraught(resultSet.getString("draught"));
            aisDataDTO.setTimestamp(resultSet.getString("timestamp"));
            aisDataDTO.setEta(resultSet.getString("eta"));
            aisDataDTO.setTheme(resultSet.getString("theme"));
            aisDataDTO.setShiptype(resultSet.getString("shiptype"));
            aisDataDTO.setImo(resultSet.getString("imo"));
            aisDataDTO.setDestination(resultSet.getString("timestamp"));
            aisDataDTO.setRadarid(resultSet.getString("radarid"));
            aisDataDTO.setReferenceid(resultSet.getString("referenceid"));
            aisDataDTO.setName(resultSet.getString("name"));
            aisDataDTO.setNavstatus(resultSet.getString("navstatus"));
            aisDataDTO.setBow(resultSet.getString("bow"));
            aisDataDTO.setStern(resultSet.getString("stern"));
            aisDataDTO.setPort(resultSet.getString("port"));
            aisDataDTO.setStarboard(resultSet.getString("starboard"));
            aisDynamicMessages.add(aisDataDTO);
        }
        return aisDynamicMessages;
    }
}
