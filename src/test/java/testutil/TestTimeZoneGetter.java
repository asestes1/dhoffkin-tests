package testutil;

import java.io.IOException;
import java.time.ZoneId;

import org.junit.Assert;
import org.junit.Test;

import util.CapacityGetter;
import util.TimeZoneGetter;

public class TestTimeZoneGetter {
	
	@Test
	public void testTimeZoneGetter() throws IOException{
		Assert.assertTrue(TimeZoneGetter.getTimeZone("DFW").equals(ZoneId.of("America/Chicago")));
		Assert.assertTrue(TimeZoneGetter.getTimeZone("DGL").equals(ZoneId.of("America/Phoenix")));
		Assert.assertTrue(TimeZoneGetter.getTimeZone("BYF").equals(ZoneId.of("Europe/Paris")));
		Assert.assertTrue(TimeZoneGetter.getTimeZone("AQP").equals(ZoneId.of("America/Lima")));


	}
	
	@Test
	public void testVfrIfrGetter() throws IOException{
		Assert.assertTrue(CapacityGetter.getIfr("DFW").equals(85));
	}
}
