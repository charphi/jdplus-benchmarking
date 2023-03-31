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

import jdplus.toolkit.base.core.ssf.benchmarking.SsfCholette;
import jdplus.benchmarking.base.api.benchmarking.univariate.Cholette;
import jdplus.benchmarking.base.api.benchmarking.univariate.CholetteSpec;
import jdplus.benchmarking.base.api.benchmarking.univariate.CholetteSpec.BiasCorrection;
import jdplus.toolkit.base.api.data.AggregationType;
import jdplus.toolkit.base.core.ssf.dk.DkToolkit;
import jdplus.toolkit.base.core.ssf.univariate.DefaultSmoothingResults;
import jdplus.toolkit.base.core.ssf.univariate.ISsf;
import jdplus.toolkit.base.core.ssf.univariate.SsfData;
import jdplus.toolkit.base.api.timeseries.TsException;
import jdplus.toolkit.base.api.timeseries.TsData;
import jdplus.toolkit.base.core.timeseries.simplets.TsDataToolkit;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.TsUnit;
import nbbrd.service.ServiceProvider;
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.api.data.DoubleSeqCursor;
import jdplus.toolkit.base.core.data.DataBlock;
import jdplus.toolkit.base.core.data.normalizer.AbsMeanNormalizer;
import jdplus.toolkit.base.core.ssf.arima.AR1;
import jdplus.toolkit.base.core.ssf.arima.Rw;
import jdplus.toolkit.base.core.ssf.ISsfLoading;
import jdplus.toolkit.base.core.ssf.StateComponent;
import jdplus.toolkit.base.core.ssf.basic.WeightedLoading;
import jdplus.toolkit.base.core.ssf.univariate.Ssf;
import static jdplus.toolkit.base.core.timeseries.simplets.TsDataToolkit.multiply;

/**
 *
 * @author Jean Palate
 */
@ServiceProvider(Cholette.Processor.class)
public class CholetteProcessor implements Cholette.Processor {

    public static final CholetteProcessor PROCESSOR = new CholetteProcessor();

    @Override
    public TsData benchmark(TsData highFreqSeries, TsData aggregationConstraint, CholetteSpec spec) {
        TsData s = correctBias(highFreqSeries, aggregationConstraint, spec);
        AbsMeanNormalizer normalizer = new AbsMeanNormalizer();
        DataBlock ns = DataBlock.of(s.getValues());
        double factor = normalizer.normalize(ns);
        TsData tmp = TsData.of(s.getStart(), ns);
        TsData btmp = cholette(tmp, aggregationConstraint.fn(z -> z * factor), spec);
        if (btmp != null) {
            btmp = btmp.fn(z -> z / factor);
        }
        return btmp;
    }

    private TsData correctBias(TsData s, TsData target, CholetteSpec spec) {
        AggregationType agg = spec.getAggregationType();
        if (spec.getBias() == BiasCorrection.None) {
            return s;
        }
        TsData sy = s.aggregate(target.getTsUnit(), agg, true);
        sy = TsDataToolkit.fitToDomain(sy, sy.getDomain().intersection(target.getDomain()));
        // TsDataBlock.all(target).data.sum() is the sum of the aggregation constraints
        //  TsDataBlock.all(sy).data.sum() is the sum of the averages or sums of the original series
        BiasCorrection bias = spec.getBias();
        if (bias == BiasCorrection.Multiplicative) {
            double b=target.getValues().sum() / sy.getValues().sum();
            return multiply(s, b);
        } else {
            double b = (target.getValues().sum() - sy.getValues().sum()) / target.length();
            return TsDataToolkit.add(s, b);
        }
    }

    /**
     *
     * @param length
     * @param ratio
     * @param agg
     * @param offset
     * @return
     */
    public static double[] expand(int length, int ratio, DoubleSeq agg, int offset) {
        // expand the data;
        double[] y = new double[length];
        for (int i = 0; i < y.length; ++i) {
            y[i] = Double.NaN;
        }
        // search the first non missing value
        int pos = offset, j = 0, m = agg.length();
        DoubleSeqCursor cursor = agg.cursor();

        while (j++ < m) {
            y[pos] = cursor.getAndNext();
            pos += ratio;
        }
        return y;
    }

    /**
     *
     * @param s
     * @param constraints
     * @return
     */
    private TsData cholette(TsData highFreqSeries, TsData aggregationConstraint, CholetteSpec spec) {
        int ratio = highFreqSeries.getTsUnit().ratioOf(aggregationConstraint.getTsUnit());
        if (ratio == TsUnit.NO_RATIO || ratio == TsUnit.NO_STRICT_RATIO) {
            throw new TsException(TsException.INCOMPATIBLE_FREQ);
        }

        TsData naggregationConstraint, agg;
        switch (spec.getAggregationType()) {
            case Sum, Average -> {
                naggregationConstraint = BenchmarkingUtility.constraints(highFreqSeries, aggregationConstraint);
                agg = highFreqSeries.aggregate(aggregationConstraint.getTsUnit(), spec.getAggregationType(), true);
            }
            case Last -> {
                naggregationConstraint = BenchmarkingUtility.constraintsByPosition(highFreqSeries, aggregationConstraint, ratio - 1);
                agg = highFreqSeries.aggregateByPosition(aggregationConstraint.getTsUnit(), ratio - 1);
            }
            case First -> {
                naggregationConstraint = BenchmarkingUtility.constraintsByPosition(highFreqSeries, aggregationConstraint, 0);
                agg = highFreqSeries.aggregateByPosition(aggregationConstraint.getTsUnit(), 0);
            }
            case UserDefined -> {
                naggregationConstraint = BenchmarkingUtility.constraintsByPosition(highFreqSeries, aggregationConstraint, spec.getObservationPosition());
                agg = highFreqSeries.aggregateByPosition(aggregationConstraint.getTsUnit(), spec.getObservationPosition());
            }
            default -> throw new TsException(TsException.INVALID_OPERATION);
        }

        TsPeriod sh = highFreqSeries.getStart();
        TsPeriod sl = TsPeriod.of(sh.getUnit(), naggregationConstraint.getStart().start());
        int offset = sh.until(sl);
        if (spec.getAggregationType() == AggregationType.Average) {
            naggregationConstraint = multiply(naggregationConstraint, ratio);
            agg = multiply(agg, ratio);
        }
        switch (spec.getAggregationType()) {
            case First -> {
            }
            case UserDefined -> offset += spec.getObservationPosition();
            default -> offset += ratio - 1;

        }

        naggregationConstraint = TsData.subtract(naggregationConstraint, agg);
        double[] y = expand(highFreqSeries.length(), ratio, naggregationConstraint.getValues(), offset);

        double[] w = null;
        if (spec.getLambda() != 0) {
            w = highFreqSeries.getValues().toArray();
            if (spec.getLambda() != 1) {
                for (int i = 0; i < w.length; ++i) {
                    w[i] = Math.pow(Math.abs(w[i]), spec.getLambda());
                }
            }
        }
        TsPeriod start = highFreqSeries.getStart();
        int head = (int) (start.getId() % ratio);
        if (spec.getAggregationType() == AggregationType.Average
                || spec.getAggregationType() == AggregationType.Sum) {
            ISsf ssf = SsfCholette.builder(ratio)
                    .start(head)
                    .rho(spec.getRho())
                    .weights(w == null ? null : DoubleSeq.of(w))
                    .build();
            DefaultSmoothingResults rslts = DkToolkit.sqrtSmooth(ssf, new SsfData(y), false, false);

            double[] b = new double[highFreqSeries.length()];
            if (w != null) {
                for (int i = 0; i < b.length; ++i) {
                    b[i] = w[i] * (rslts.a(i).get(1));
                }
            } else {
                rslts.getComponent(1).copyTo(b, 0);
            }
            return TsData.add(highFreqSeries, TsData.ofInternal(start, b));
        } else {
            ISsfLoading loading;
            StateComponent cmp;
            if (spec.getRho() == 1) {
                loading = Rw.defaultLoading();
                cmp = Rw.DEFAULT;
            } else {
                loading = AR1.defaultLoading();
                cmp = AR1.of(spec.getRho());
            }
            if (w != null) {
                double[] weights = w;
                loading = WeightedLoading.of(loading, i -> weights[i]);
            }
            ISsf ssf = Ssf.of(cmp, loading);
            DefaultSmoothingResults rslts = DkToolkit.smooth(ssf, new SsfData(y), false, false);
            double[] b = new double[highFreqSeries.length()];
            for (int i = 0; i < b.length; ++i) {
                b[i] = loading.ZX(i, rslts.a(i));
            }
            return TsData.add(highFreqSeries, TsData.ofInternal(start, b));
        }
    }

}
