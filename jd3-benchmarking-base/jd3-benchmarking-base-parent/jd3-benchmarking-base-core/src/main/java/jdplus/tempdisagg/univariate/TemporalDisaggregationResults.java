/*
 * Copyright 2019 National Bank of Belgium.
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *      https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdplus.tempdisagg.univariate;

import demetra.data.DoubleSeq;
import demetra.information.GenericExplorable;
import nbbrd.design.Development;
import demetra.math.functions.ObjectiveFunctionPoint;
import demetra.timeseries.TsData;
import demetra.timeseries.TsDomain;
import demetra.timeseries.regression.Variable;
import demetra.math.matrices.Matrix;
import jdplus.stats.likelihood.DiffuseConcentratedLikelihood;
import jdplus.stats.likelihood.DiffuseLikelihoodStatistics;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
@lombok.Value
@lombok.Builder
@Development(status = Development.Status.Alpha)
public class TemporalDisaggregationResults implements GenericExplorable{

    @lombok.NonNull
    TsData originalSeries;
    
    @lombok.NonNull
    TsDomain disaggregationDomain;
    
    /**
     * Regression variables
     */
    private Variable[] indicators;
    
    
    /**
     * Regression estimation. The order correspond to the order of the variables
     * 
     */
    int hyperParametersCount;
    
    DiffuseConcentratedLikelihood likelihood;
    DiffuseLikelihoodStatistics stats;
    
    public DoubleSeq getCoefficients(){
        return likelihood.coefficients();
    }
    
    public Matrix getCoefficientsCovariance(){
        return likelihood.covariance(hyperParametersCount, true);
    }
    
    ObjectiveFunctionPoint maximum;
    
    ResidualsDiagnostics residualsDiagnostics;

    @lombok.NonNull
    TsData disaggregatedSeries;
    
    @lombok.NonNull
    TsData stdevDisaggregatedSeries;
    
    TsData regressionEffects;
    
}
