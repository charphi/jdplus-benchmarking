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
package jdplus.benchmarking.base.api.multivariate;

import jdplus.benchmarking.base.api.benchmarking.multivariate.ContemporaneousConstraint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Jean Palate
 */
public class ContemporaneousConstraintTest {
    
    public ContemporaneousConstraintTest() {
    }

    @Test
    public void testWc() {
        ContemporaneousConstraint cnt=ContemporaneousConstraint.parse("y=s*");
        cnt=cnt.expand(Arrays.asList("s1", "s2", "s11", "y", "a"));
        assertEquals(cnt.getComponents().size(),3);
    }
    
}
