/*
 * Copyright 2022 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package jdplus.benchmarking.base.r;

import jdplus.benchmarking.base.api.calendarization.CalendarizationResults;
import jdplus.toolkit.base.api.timeseries.CalendarTimeSeries;
import jdplus.toolkit.base.r.timeseries.TsUtility;
import org.junit.jupiter.api.Test;

/**
 *
 * @author PALATEJ
 */
public class CalendarizationTest {

    public CalendarizationTest() {
    }

    @Test
    public void testSomeMethod() {
        String[] starts = new String[]{
            "1980-01-01", "1983-01-01", "1986-01-01", "1988-01-01"
        };
        String[] ends = new String[]{
            "1982-12-31", "1985-12-31", "1987-12-31", "2000-12-31"
        };
        double[] data = new double[]{100, -100, 10, 20};
        CalendarTimeSeries ts = TsUtility.of(starts, ends, data);
        CalendarizationResults rslt = Calendarization.process(ts, 12, null, "2001-11-30", null, true);
//        System.out.println(rslt.getAggregatedSeries());
    }

}
