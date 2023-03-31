/*
 * Copyright 2017 National Bank of Belgium
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
package jdplus.benchmarking.base.core.benchmarking.univariate;

import jdplus.benchmarking.base.api.benchmarking.univariate.Denton;
import jdplus.benchmarking.base.api.benchmarking.univariate.DentonSpec;
import jdplus.toolkit.base.api.timeseries.TsException;
import jdplus.toolkit.base.api.timeseries.TsUnit;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.TsData;
import nbbrd.service.ServiceProvider;

/**
 *
 * @author Jean Palate
 */
@ServiceProvider(Denton.Processor.class)
public class DentonProcessor implements Denton.Processor {

    public static final DentonProcessor PROCESSOR=new DentonProcessor();

    @Override
    public TsData benchmark(TsData highFreqSeries, TsData aggregationConstraint, DentonSpec spec) {
        int ratio = highFreqSeries.getTsUnit().ratioOf(aggregationConstraint.getTsUnit());
        if (ratio == TsUnit.NO_RATIO || ratio == TsUnit.NO_STRICT_RATIO) {
            throw new TsException(TsException.INCOMPATIBLE_FREQ);
        }
        
        TsData naggregationConstraint;
        switch (spec.getAggregationType()){
            case Sum, Average -> naggregationConstraint=BenchmarkingUtility.constraints(highFreqSeries, aggregationConstraint);
            case Last -> naggregationConstraint=BenchmarkingUtility.constraintsByPosition(highFreqSeries, aggregationConstraint, ratio-1);
            case First -> naggregationConstraint=BenchmarkingUtility.constraintsByPosition(highFreqSeries, aggregationConstraint, 0);
            case UserDefined -> naggregationConstraint=BenchmarkingUtility.constraintsByPosition(highFreqSeries, aggregationConstraint, spec.getObservationPosition());
            default -> throw new TsException(TsException.INVALID_OPERATION);
        }
        
  
        TsPeriod sh = highFreqSeries.getStart();
        TsPeriod sl = TsPeriod.of(sh.getUnit(), naggregationConstraint.getStart().start());
        int offset = sh.until(sl);
        MatrixDenton denton = new MatrixDenton(spec, ratio, offset);
        double[] r = denton.process(highFreqSeries.getValues(), naggregationConstraint.getValues());
        return TsData.ofInternal(sh, r);
    }

    @Override
    public TsData benchmark(TsUnit highFreq, TsData aggregationConstraint, DentonSpec spec) {
        int ratio = highFreq.ratioOf(aggregationConstraint.getTsUnit());
        if (ratio == TsUnit.NO_RATIO || ratio == TsUnit.NO_STRICT_RATIO) {
            throw new TsException(TsException.INCOMPATIBLE_FREQ);
        }
        // Y is limited to q !
        TsPeriod sh = TsPeriod.of(highFreq, aggregationConstraint.getStart().start());
        MatrixDenton denton = new MatrixDenton(spec, ratio, 0);
        double[] r = denton.process(aggregationConstraint.getValues());
        return TsData.ofInternal(sh, r);
    }


}
