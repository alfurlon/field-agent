package learn.field_agent.data;

import learn.field_agent.data.mappers.AgencyAgentMapper;
import learn.field_agent.data.mappers.SecurityClearanceMapper;
import learn.field_agent.domain.Result;
import learn.field_agent.domain.ResultType;
import learn.field_agent.models.AgencyAgent;
import learn.field_agent.models.SecurityClearance;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class SecurityClearanceJdbcTemplateRepository implements SecurityClearanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public SecurityClearanceJdbcTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SecurityClearance> findAll() {

        final String sql = "select security_clearance_id, name security_clearance_name "
                + "from security_clearance;";
        return jdbcTemplate.query(sql, new SecurityClearanceMapper());
    }

    @Override
    public SecurityClearance findById(int securityClearanceId) {

        final String sql = "select security_clearance_id, name security_clearance_name "
                + "from security_clearance "
                + "where security_clearance_id = ?;";

        return jdbcTemplate.query(sql, new SecurityClearanceMapper(), securityClearanceId)
                .stream()
                .findFirst().orElse(null);
    }

    @Override
    public SecurityClearance add(SecurityClearance securityClearance) {

        final String sql = "insert into security_clearance (name) values (?);";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, securityClearance.getName());
            return ps;
        }, keyHolder);

        if (rowsAffected <= 0) {
            return null;
        }

        securityClearance.setSecurityClearanceId(keyHolder.getKey().intValue());
        return securityClearance;
    }

    @Override
    public boolean update(SecurityClearance securityClearance) {

        final String sql = "update security_clearance set "
                + "name = ? "
                + "where security_clearance_id = ?;";

        return jdbcTemplate.update(sql,
                securityClearance.getName(),
                securityClearance.getSecurityClearanceId()) > 0;
    }

    @Override
    public Result<SecurityClearance> deleteById(int securityClearanceId) {
        Result<SecurityClearance> result = new Result<>();
        SecurityClearance securityClearance = findById(securityClearanceId);
        if (securityClearance == null) {
            result.addMessage("securityClearance not found.", ResultType.NOT_FOUND);
            return result;
        }
        List<Integer> agencyAgents = checkForReference(securityClearance);

        if (agencyAgents.get(0) > 0) {
            result.addMessage("Cannot delete security clearance that is in use.", ResultType.INVALID);
            return result;
        }

        if (jdbcTemplate.update("delete from security_clearance where security_clearance_id = ?;", securityClearanceId) > 0) {
            result.addMessage("Success", ResultType.SUCCESS);
        }

        return result;
    }

    private List<Integer> checkForReference(SecurityClearance securityClearance) {

        final String sql = "select count(*) from agency_agent where security_clearance_id = ?;";

        var count = jdbcTemplate.query(sql,
                intMapper,
                securityClearance.getSecurityClearanceId());
        return count;
    }

    private final RowMapper<Integer> intMapper = (resultSet, i) -> {
        int count = 0;
        count = resultSet.getInt("count(*)");
        return count;
    };
}
