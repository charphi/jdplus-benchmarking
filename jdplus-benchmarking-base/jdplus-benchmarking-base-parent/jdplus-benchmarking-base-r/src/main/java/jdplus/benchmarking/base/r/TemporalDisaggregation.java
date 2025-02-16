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

import jdplus.toolkit.base.api.data.AggregationType;
import jdplus.toolkit.base.api.data.Parameter;
import jdplus.toolkit.base.api.ssf.SsfInitialization;
import jdplus.benchmarking.base.api.univariate.ModelBasedDentonSpec;
import jdplus.benchmarking.base.core.univariate.TemporalDisaggregationIResults;
import jdplus.benchmarking.base.api.univariate.TemporalDisaggregationISpec;
import jdplus.benchmarking.base.core.univariate.TemporalDisaggregationResults;
import jdplus.benchmarking.base.api.univariate.TemporalDisaggregationSpec;
import jdplus.benchmarking.base.api.univariate.TemporalDisaggregationSpec.Model;
import jdplus.toolkit.base.api.timeseries.TsData;
import jdplus.toolkit.base.api.timeseries.TsDomain;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.TsUnit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jdplus.benchmarking.base.core.univariate.ModelBasedDentonProcessor;
import jdplus.benchmarking.base.core.univariate.ModelBasedDentonResults;
import jdplus.benchmarking.base.core.univariate.ProcessorI;
import jdplus.benchmarking.base.core.univariate.TemporalDisaggregationProcessor;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class TemporalDisaggregation {


    public TemporalDisaggregationIResults processI(TsData y, TsData indicator, String model, String aggregation, int obspos,
            double rho, boolean fixedrho, double truncatedRho) {
        TemporalDisaggregationISpec spec = TemporalDisaggregationISpec.builder()
                .constant(true)
                .residualsModel(Model.valueOf(model))
                .aggregationType(AggregationType.valueOf(aggregation))
                .observationPosition(obspos)
                .parameter(fixedrho ? Parameter.fixed(rho) : Parameter.initial(rho))
                .truncatedRho(truncatedRho)
                .build();
        return ProcessorI.process(y, indicator, spec);
    }
    
    public ModelBasedDentonResults processModelBasedDenton(TsData y, TsData indicator, int differencing, String aggregation, int obspos, String[] odates, double[] ovar, String[] fdates, double[] fval){
        ModelBasedDentonSpec.Builder builder = ModelBasedDentonSpec.builder()
                .aggregationType(AggregationType.valueOf(aggregation));
        if (odates != null && ovar != null){
            if (odates.length != ovar.length)
                throw new IllegalArgumentException();
            for (int i=0; i<odates.length; ++i){
                builder.shockVariance(LocalDate.parse(odates[i], DateTimeFormatter.ISO_DATE), ovar[i]);
            }
        }  
        if (fdates != null && fval != null){
            if (fdates.length != fval.length)
                throw new IllegalArgumentException();
            for (int i=0; i<fdates.length; ++i){
                builder.fixedBiRatio(LocalDate.parse(fdates[i], DateTimeFormatter.ISO_DATE), fval[i]);
            }
        }  
        return ModelBasedDentonProcessor.process(y, indicator, builder.build());
    }

    public TemporalDisaggregationResults process(TsData y, boolean constant, boolean trend, TsData[] indicators,
            String model, int freq, String aggregation, int obspos,
            double rho, boolean fixedrho, double truncatedRho, boolean zeroinit,
            String algorithm, boolean diffuseregs) {
        TemporalDisaggregationSpec.Builder builder = TemporalDisaggregationSpec.builder()
                .constant(constant)
                .trend(trend)
                .residualsModel(TemporalDisaggregationSpec.Model.valueOf(model))
                .aggregationType(AggregationType.valueOf(aggregation))
                .parameter(fixedrho ? Parameter.fixed(rho) : Parameter.initial(rho))
                .truncatedParameter(truncatedRho <= -1 ? null : truncatedRho)
                .algorithm(SsfInitialization.valueOf(algorithm))
                .zeroInitialization(zeroinit)
                .diffuseRegressors(diffuseregs)
                .rescale(true);
        if (aggregation.equals("UserDefined")) {
            builder.observationPosition(obspos);
        }
        if (indicators == null) {
            TsUnit unit = TsUnit.ofAnnualFrequency(freq);
            TsPeriod start = TsPeriod.of(unit, y.getStart().start());
            TsPeriod end = TsPeriod.of(unit, y.getDomain().end());
            TsDomain all = TsDomain.of(start, start.until(end) + 2 * freq);
            return TemporalDisaggregationProcessor.process(y, all, builder.build());
        } else {
            for (int i = 0; i < indicators.length; ++i) {
                indicators[i] = indicators[i].cleanExtremities();
            }
            return TemporalDisaggregationProcessor.process(y, indicators, builder.build());
        }
    }

}
