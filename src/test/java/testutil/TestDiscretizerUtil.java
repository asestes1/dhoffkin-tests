package testutil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.Assert;
import org.junit.Test;

import util.DiscretizerUtil;

public class TestDiscretizerUtil {
	
	@Test
	public void testDurationToTimePeriods(){
		Assert.assertEquals(DiscretizerUtil.durationToTimePeriods(Duration.ofMinutes(50),Duration.ofMinutes(10)), 5);
		Assert.assertEquals(DiscretizerUtil.durationToTimePeriods(Duration.ofMinutes(55),Duration.ofMinutes(10)), 5);
		Assert.assertEquals(DiscretizerUtil.durationToTimePeriods(Duration.ofMinutes(60),Duration.ofMinutes(10)), 6);
		Assert.assertEquals(DiscretizerUtil.durationToTimePeriods(Duration.ofMinutes(36),Duration.ofMinutes(12)), 3);
		Assert.assertEquals(DiscretizerUtil.durationToTimePeriods(Duration.ofMinutes(34),Duration.ofMinutes(12)), 2);
		Assert.assertEquals(DiscretizerUtil.durationToTimePeriods(Duration.ofMinutes(0),Duration.ofMinutes(12)), 0);



	}
	
	@Test
	public void testTimeToIndex(){
		Assert.assertEquals(DiscretizerUtil.timeToIndex(
				LocalDateTime.of(2017, 7, 15, 15, 0).atOffset(ZoneOffset.UTC),
				LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(ZoneOffset.UTC),
				Duration.ofMinutes(10)), 18);
		Assert.assertEquals(
				DiscretizerUtil.timeToIndex(LocalDateTime.of(2017, 7, 15, 15, 1).atOffset(
						ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						 Duration.ofMinutes(10)), 18);
		Assert.assertEquals(
				DiscretizerUtil.timeToIndex(
						LocalDateTime.of(2017, 7, 15, 15, 10).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						 Duration.ofMinutes(10)), 19);

		Assert.assertEquals(
				DiscretizerUtil.timeToIndex(
						LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						 Duration.ofMinutes(15)), 96);
		Assert.assertEquals(
				DiscretizerUtil.timeToIndex(
						LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						 Duration.ofMinutes(60)), 24);
		Assert.assertEquals(
				DiscretizerUtil.timeToIndex(
						LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 12, 1).atOffset(
								ZoneOffset.UTC),
						 Duration.ofMinutes(60)), 23);
		Assert.assertEquals(
				DiscretizerUtil.timeToIndex(
						LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 13, 0).atOffset(
								ZoneOffset.UTC),
						 Duration.ofMinutes(60)), 23);
	}
	
	@Test
	public void testGetNumTimePeriods() {
		Assert.assertEquals(
				DiscretizerUtil.getNumTimePeriods(
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 15, 0).atOffset(
								ZoneOffset.UTC), Duration.ofMinutes(10)), 18);
		Assert.assertEquals(
				DiscretizerUtil.getNumTimePeriods(
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 15, 1).atOffset(
								ZoneOffset.UTC), Duration.ofMinutes(10)), 19);
		Assert.assertEquals(
				DiscretizerUtil.getNumTimePeriods(
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 15, 15, 10).atOffset(
								ZoneOffset.UTC), Duration.ofMinutes(10)), 19);

		Assert.assertEquals(
				DiscretizerUtil.getNumTimePeriods(
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(
								ZoneOffset.UTC), Duration.ofMinutes(15)), 96);
		Assert.assertEquals(
				DiscretizerUtil.getNumTimePeriods(
						LocalDateTime.of(2017, 7, 15, 12, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(
								ZoneOffset.UTC), Duration.ofMinutes(60)), 24);
		Assert.assertEquals(
				DiscretizerUtil.getNumTimePeriods(
						LocalDateTime.of(2017, 7, 15, 12, 1).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(
								ZoneOffset.UTC), Duration.ofMinutes(60)), 24);
		Assert.assertEquals(
				DiscretizerUtil.getNumTimePeriods(
						LocalDateTime.of(2017, 7, 15, 13, 0).atOffset(
								ZoneOffset.UTC),
						LocalDateTime.of(2017, 7, 16, 12, 0).atOffset(
								ZoneOffset.UTC), Duration.ofMinutes(60)), 23);
	}
}
