package fr.volkaert.event_broker.test_server.tests;

import fr.volkaert.event_broker.test_server.model.TestRecord;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/tests/recordings")
public class TestRecordingService {

    List<TestRecord> records = new ArrayList<>();

    public synchronized void addRecord(TestRecord record) {
        records.add(0, record);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public synchronized List<TestRecord> getRecords(@RequestParam(required = false) String status) {
        if (status == null) {
            return new ArrayList<>(records);
        }
        else {
            boolean filterWithStatusSuccess =   status.equalsIgnoreCase("success") ||
                                                status.equalsIgnoreCase("succeed") ||
                                                status.equalsIgnoreCase("succeeded") ||
                                                status.equalsIgnoreCase("successful") ||
                                                status.equalsIgnoreCase("passed") ||
                                                status .equalsIgnoreCase("ok");
            boolean filterWithStatusFailure =   status.equalsIgnoreCase("failure") ||
                                                status.equalsIgnoreCase("failed") ||
                                                status.equalsIgnoreCase("fail") ||
                                                status.equalsIgnoreCase("error") ||
                                                status.equalsIgnoreCase("ko");
            if (! filterWithStatusSuccess && ! filterWithStatusFailure) {
                return new ArrayList<>(records);
            }
            List<TestRecord> filteredRecords = new ArrayList<>();
            for (TestRecord record : records) {
                if (record.isFinished() && ((filterWithStatusSuccess && record.isSucceeded()) || (filterWithStatusFailure && !record.isSucceeded())))
                    filteredRecords.add(record);
            }
            return filteredRecords;
        }
    }
}
