package uk.org.opentrv.test.ETV;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Supplier;

import org.junit.Test;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationInput;
import uk.org.opentrv.ETV.parse.NBulkInputs;
import uk.org.opentrv.ETV.parse.NBulkKWHParseByID;
import uk.org.opentrv.hdd.Util;
import uk.org.opentrv.test.hdd.DDNExtractorTest;

public class ETVParseTest
    {
    /**Sample 1 of bulk energy readings; too small a sample to extract a whole day's readings from. */
    public static final String sampleN1 =
        "house_id,received_timestamp,device_timestamp,energy,temperature\n" +
        "1002,1456790560,1456790400,306.48,-3\n" +
        "1002,1456791348,1456791300,306.48,-3\n" +
        "1002,1456792442,1456792200,306.48,-3\n";

    /**Test bulk gas meter parse basics. */
    @Test public void testNBulkParseBasics() throws IOException
        {
        // Rudimentary test of bad-arg checking.
        try { new NBulkKWHParseByID(-1, null); fail(); } catch(final IllegalArgumentException e) { /* OK */ }
        // Check that just a header, or no matching entries, returns empty rather than an exception.
        assertTrue(new NBulkKWHParseByID(0, new StringReader("house_id,received_timestamp,device_timestamp,energy,temperature")).getKWhByLocalDay().isEmpty());

        // Check correct number of rows read with wrong/right ID chosen
        // and only using data for full local-time day intervals.
        assertEquals(0, new NBulkKWHParseByID(0, new StringReader(sampleN1)).getKWhByLocalDay().size());
        assertEquals(0, new NBulkKWHParseByID(1002, new StringReader(sampleN1)).getKWhByLocalDay().size());
        }

    /**Name of the ETV sample bulk HDD data for EGLL. */
    public static final String N_BULK_DATA_FORMAT_SAMPLE_CSV = "N-bulk-data-format-sample.csv";
    /**Return a Reader for the ETV sample bulk HDD data for EGLL; never null. */
    public static Reader getNBulk1CSVReader() throws IOException
        { return(Util.getASCIIResourceReader(ETVParseTest.class, N_BULK_DATA_FORMAT_SAMPLE_CSV)); }
    /**Return a Supplier<Reader> for the ETV sample bulk HDD data for EGLL; never null. */
    public static Supplier<Reader> NBulk1CSVReaderSupplier = Util.getASCIIResourceReaderSupplier(ETVParseTest.class, N_BULK_DATA_FORMAT_SAMPLE_CSV);

    /**Test bulk gas meter parse on a more substantive sample. */
    @Test public void testNBulkParse() throws IOException
        {
        // Check correct number of rows read with wrong/right ID chosen
        // and only using data for full local-time day intervals.
        assertEquals(0, new NBulkKWHParseByID(0, getNBulk1CSVReader()).getKWhByLocalDay().size());
        final SortedMap<Integer, Float> kwhByLocalDay1002 = new NBulkKWHParseByID(1002, getNBulk1CSVReader()).getKWhByLocalDay();
        assertEquals(1, kwhByLocalDay1002.size());
        assertTrue(kwhByLocalDay1002.containsKey(20160301));
        assertEquals(75.31f, kwhByLocalDay1002.get(20160301), 0.01f);
        // Check correct ID extraction.
        assertEquals(2, NBulkKWHParseByID.extractIDs(getNBulk1CSVReader()).size());
        assertTrue(NBulkKWHParseByID.extractIDs(getNBulk1CSVReader()).contains(1001));
        assertTrue(NBulkKWHParseByID.extractIDs(getNBulk1CSVReader()).contains(1002));
        }

    /**Name of the ETV sample bulk HDD data for EGLL. */
    public static final String N_BULK_DATA_FORMAT_SAMPLE_CONCAT_CSV = "N-bulk-data-format-sample-concat.csv";
    /**Return a Reader for the ETV sample bulk HDD data for EGLL; never null. */
    public static Reader getNBulk1ConcatCSVReader() throws IOException
        { return(Util.getASCIIResourceReader(ETVParseTest.class, N_BULK_DATA_FORMAT_SAMPLE_CONCAT_CSV)); }

    /**Test bulk gas meter parse on a more substantive sample of concatenated files. */
    @Test public void testNBulkParseConcat() throws IOException
        {
        // Check correct number of rows read with wrong/right ID chosen
        // and only using data for full local-time day intervals.
        assertEquals(0, new NBulkKWHParseByID(0, getNBulk1ConcatCSVReader()).getKWhByLocalDay().size());
        final SortedMap<Integer, Float> kwhByLocalDay1002 = new NBulkKWHParseByID(1002, getNBulk1CSVReader()).getKWhByLocalDay();
        assertEquals(1, kwhByLocalDay1002.size());
        assertTrue(kwhByLocalDay1002.containsKey(20160301));
        assertEquals(75.31f, kwhByLocalDay1002.get(20160301), 0.01f);
        // Check correct ID extraction.
        assertEquals(2, NBulkKWHParseByID.extractIDs(getNBulk1CSVReader()).size());
        assertTrue(NBulkKWHParseByID.extractIDs(getNBulk1CSVReader()).contains(1001));
        assertTrue(NBulkKWHParseByID.extractIDs(getNBulk1CSVReader()).contains(1002));
        }

    /**Test bulk gas meter parse for multiple households at once. */
    @Test public void testNBulkParseMulti() throws IOException
        {
        final Map<String, ETVPerHouseholdComputationInput> mhi =
            NBulkInputs.gatherDataForAllHouseholds(
                NBulk1CSVReaderSupplier,
                DDNExtractorTest.getETVEGLLHDD2016H1CSVReader());
        assertNotNull(mhi);
        assertEquals(2, mhi.size());
        assertTrue(mhi.containsKey("1001"));
        assertTrue(mhi.containsKey("1002"));
        assertEquals("1001", mhi.get("1001").getHouseID());
        assertEquals("1002", mhi.get("1002").getHouseID());
        }

    /**Sample 2 of bulk energy readings; a few-days' values all at or close after midnight. */
    public static final String sampleN2 =
        "house_id,received_timestamp,device_timestamp,energy,temperature\n" +
        "1002,1456790560,1456790400,306.48,-3\n" +
        "1002,1456791348,1456791300,306.48,-3\n" +
        "1002,1456877005,1456876800,381.79,-1\n" +
        "1002,1456963400,1456963200,454.89,0\n" +
        "1002,1457049600,1457049600,488.41,1\n" +
        "1002,1457050500,1457050500,532.29,0\n";

    /**Test that correct samples used when multiple eligible are present, for several days' data. */
    @Test public void testNBulkParse2() throws IOException
        {
        // Check correct number of rows read with wrong/right ID chosen
        // and only using data for full local-time day intervals.
        assertEquals(0, new NBulkKWHParseByID(1001, new StringReader(sampleN2)).getKWhByLocalDay().size());
        final SortedMap<Integer, Float> kwhByLocalDay1002 = new NBulkKWHParseByID(1002, new StringReader(sampleN2)).getKWhByLocalDay();
        assertEquals(3, kwhByLocalDay1002.size());
        // Check that the 00:00 samples are used
        // even when other close/eligible ones are present.
        assertTrue(kwhByLocalDay1002.containsKey(20160301));
        assertEquals(75.31f, kwhByLocalDay1002.get(20160301), 0.01f);
        assertTrue(kwhByLocalDay1002.containsKey(20160302));
        assertEquals(73.1f, kwhByLocalDay1002.get(20160302), 0.01f);
        assertTrue(kwhByLocalDay1002.containsKey(20160303));
        assertEquals(33.52f, kwhByLocalDay1002.get(20160303), 0.01f);
        // Check correct ID extraction.
        assertEquals(1, NBulkKWHParseByID.extractIDs(new StringReader(sampleN2)).size());
        assertEquals(1002, NBulkKWHParseByID.extractIDs(new StringReader(sampleN2)).iterator().next().intValue());
        }

    /**Sample 3 of bulk energy readings in UK; values around clocks going DST switch. */
    public static final String sampleN3 =
        "house_id,received_timestamp,device_timestamp,energy,temperature\n" +
        "1002,1458864000,1458864000,10,0\n" + // TZ='Europe/London' date +%s --date='2016/03/25 00:00'
        "1002,1458950400,1458950400,21,0\n" + // TZ='Europe/London' date +%s --date='2016/03/26 00:00'
        "1002,1459036800,1459036800,33,0\n" + // TZ='Europe/London' date +%s --date='2016/03/27 00:00'
        // Clocks go forward, so 23h interval here rather than usual 24h...
        "1002,1459119600,1459119600,46,0\n" + // TZ='Europe/London' date +%s --date='2016/03/28 00:00'
        // Offer up (wrong) 24h interval which should be ignored.
        "1002,1459123200,1459123200,47,0\n";  // TZ='Europe/London' date +%s --date='2016/03/28 01:00'

    // Note helpful *nx tools, eg date:
    //     date --date='@2147483647'
    //     TZ='Europe/London' date
    //     date +%s

    /**Test for correct behaviour around daylight-savings change.
     * HDD runs local time midnight-to-midnight so the energy interval should do so too.
     */
    @Test public void testNBulkParse3() throws IOException
        {
        // Check correct number of rows read with wrong/right ID chosen
        // and only using data for full local-time day intervals.
        assertEquals(0, new NBulkKWHParseByID(9999, new StringReader(sampleN3)).getKWhByLocalDay().size());
        final SortedMap<Integer, Float> kwhByLocalDay1002 = new NBulkKWHParseByID(1002, new StringReader(sampleN3)).getKWhByLocalDay();
        assertEquals(3, kwhByLocalDay1002.size());
        // Check that the 00:00 samples are used
        // even when other close/eligible ones are present.
        assertTrue(kwhByLocalDay1002.containsKey(20160325));
        assertEquals(11f, kwhByLocalDay1002.get(20160325), 0.01f);
        assertTrue(kwhByLocalDay1002.containsKey(20160326));
        assertEquals(12f, kwhByLocalDay1002.get(20160326), 0.01f);
        assertTrue(kwhByLocalDay1002.containsKey(20160327));
        assertEquals(13f, kwhByLocalDay1002.get(20160327), 0.01f);
        }

    /**Test for correct loading for a single household into input object. */
    @Test public void testNBulkInputs() throws IOException
        {
        final ETVPerHouseholdComputationInput data = NBulkInputs.gatherData(1002, getNBulk1CSVReader(), DDNExtractorTest.getETVEGLLHDD201603CSVReader());
        assertNotNull(data);
        assertEquals("1002", data.getHouseID());
        assertEquals(1, data.getKWhByLocalDay().size());
        assertTrue(data.getKWhByLocalDay().containsKey(20160301));
        assertEquals(75.31f, data.getKWhByLocalDay().get(20160301), 0.01f);
        assertEquals(31, data.getHDDByLocalDay().size());
        assertEquals(10.1f, data.getHDDByLocalDay().get(20160302), 0.01f);
        assertEquals(7.9f, data.getHDDByLocalDay().get(20160329), 0.01f);
        }

    /**Return a stream for the ETV (ASCII) sample single-home bulk kWh consumption data; never null. */
    public static InputStream getNBulkSHCSVStream()
        { return(ETVParseTest.class.getResourceAsStream("N-sample-GAS-2016-07.csv")); }
    /**Return a Reader for the ETV sample single-home bulk HDD data for EGLL; never null. */
    public static Reader getNBulkSHCSVReader() throws IOException
        { return(new InputStreamReader(getNBulkSHCSVStream(), "ASCII7")); }

    /**Test for correct loading for a single household into input object from alternative bulk file. */
    @Test public void testNBulkSHInputs() throws IOException
        {
        final ETVPerHouseholdComputationInput data = NBulkInputs.gatherData(5013, getNBulkSHCSVReader(), DDNExtractorTest.getETVEGLLHDD2016H1CSVReader());
        assertNotNull(data);
        assertEquals("5013", data.getHouseID());
        assertEquals(187, data.getKWhByLocalDay().size());
        assertEquals(182, data.getHDDByLocalDay().size());
        assertTrue(data.getKWhByLocalDay().containsKey(20160216));
        assertEquals(28.43f, data.getKWhByLocalDay().get(20160216), 0.01f);
        assertTrue(data.getKWhByLocalDay().containsKey(20160319));
        assertEquals(19.33f, data.getKWhByLocalDay().get(20160319), 0.01f);
        // Data around DST switch.
        assertEquals(17.10f, data.getKWhByLocalDay().get(20160320), 0.01f);
        assertEquals(9.33f, data.getKWhByLocalDay().get(20160322), 0.01f);
        assertEquals(10.11f, data.getKWhByLocalDay().get(20160325), 0.01f);
        assertEquals(16.11f, data.getKWhByLocalDay().get(20160326), 0.015f); // Needs extra error margin...
        assertEquals(9.00f, data.getKWhByLocalDay().get(20160327), 0.01f);  // Spring forward...
        assertEquals(12.66f, data.getKWhByLocalDay().get(20160328), 0.01f);
        assertEquals(10.55f, data.getKWhByLocalDay().get(20160331), 0.01f);
        }


    /**Name of the ETV (ASCII) sample single-home bulk kWh consumption data all in 2016H1. */
    public static final String N_SAMPLE_GAS_2016_06_CSV = "N-sample-GAS-2016-06.csv";
    /**Return a Reader for the ETV sample bulk HDD data for EGLL; never null. */
    public static Reader getNBulkSH2016H1CSVReader() throws IOException
        { return(Util.getASCIIResourceReader(ETVParseTest.class, N_SAMPLE_GAS_2016_06_CSV)); }
    /**Return a Supplier<Reader> for the ETV sample bulk HDD data for EGLL; never null. */
    public static Supplier<Reader> NBulkSH2016H1CSVReaderSupplier = Util.getASCIIResourceReaderSupplier(ETVParseTest.class, N_SAMPLE_GAS_2016_06_CSV);

    /**Test for correct loading for a single household into input object from alternative bulk file (2016H1). */
    @Test public void testNBulkSH2016HCInputs() throws IOException
        {
        final ETVPerHouseholdComputationInput data = NBulkInputs.gatherData(5013, getNBulkSH2016H1CSVReader(), DDNExtractorTest.getETVEGLLHDD2016H1CSVReader());
        assertNotNull(data);
        assertEquals("5013", data.getHouseID());
        assertEquals(156, data.getKWhByLocalDay().size());
        assertEquals(182, data.getHDDByLocalDay().size());
        assertTrue(data.getKWhByLocalDay().containsKey(20160216));
        assertEquals(28.43f, data.getKWhByLocalDay().get(20160216), 0.01f);
        assertTrue(data.getKWhByLocalDay().containsKey(20160630));
        assertFalse(data.getKWhByLocalDay().containsKey(20160701)); // Data runs up to 20160630.
        }

    /**Test bulk gas meter parse via multi-household route. */
    @Test public void testNBulkSHMultiInputs() throws IOException
        {
        final Map<String, ETVPerHouseholdComputationInput> mhi =
            NBulkInputs.gatherDataForAllHouseholds(
                    NBulkSH2016H1CSVReaderSupplier,
                DDNExtractorTest.getETVEGLLHDD2016H1CSVReader());
        assertNotNull(mhi);
        assertEquals(1, mhi.size());
        assertTrue(mhi.containsKey("5013"));
        assertEquals("5013", mhi.get("5013").getHouseID());
        }

    /**Test bulk multi-household reader. */
    @Test public void testNBulkMultiInputs() throws IOException
        {
        final Map<String, ETVPerHouseholdComputationInput> mhi =
            NBulkInputs.gatherDataForAllHouseholds(
                    NBulk1CSVReaderSupplier,
                DDNExtractorTest.getETVEGLLHDD2016H1CSVReader());
        assertNotNull(mhi);
        assertEquals(2, mhi.size());
        assertTrue(mhi.containsKey("1001"));
        assertTrue(mhi.containsKey("1002"));
        assertEquals("1001", mhi.get("1001").getHouseID());
        assertEquals("1002", mhi.get("1002").getHouseID());
        }
    }
