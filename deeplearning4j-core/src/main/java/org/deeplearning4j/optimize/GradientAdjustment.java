/*
 * Copyright 2015 Skymind,Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.deeplearning4j.optimize;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.AdaGrad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gradient adjustment
 * @author Adam Gibson
 */
public class GradientAdjustment {

    private GradientAdjustment(){}


    private static Logger log = LoggerFactory.getLogger(GradientAdjustment.class);

    /**
     * Update the gradient according to the configuration such as adagrad, momentum, and sparsity
     * @param gradient the gradient to modify
     */
    public static void updateGradientAccordingToParams(NeuralNetConfiguration conf,int iteration,AdaGrad adaGrad,INDArray gradient,INDArray params,int batchSize) {
        if(adaGrad == null)
            adaGrad = new AdaGrad(1,gradient.length());


        //reset adagrad history
        if(iteration != 0 && conf.getResetAdaGradIterations() > 0 &&  iteration % conf.getResetAdaGradIterations() == 0) {
            adaGrad.historicalGradient = null;

            log.info("Resetting adagrad");
        }

        //change up momentum after so many iterations if specified
        double momentum = conf.getMomentum();
        if(conf.getMomentumAfter() != null && !conf.getMomentumAfter().isEmpty()) {
            int key = conf.getMomentumAfter().keySet().iterator().next();
            if(iteration >= key) {
                momentum = conf.getMomentumAfter().get(key);
            }
        }


        gradient = adaGrad.getGradient(gradient);
        if (conf.isUseAdaGrad())
            gradient.assign(adaGrad.getGradient(gradient));

        else
            gradient.muli(conf.getLr());





        if (momentum > 0)
            gradient.addi(gradient.mul(momentum).addi(gradient.mul(1 - momentum)));

        //simulate post gradient application  and apply the difference to the gradient to decrease the change the gradient has
        if(conf.isUseRegularization() && conf.getL2() > 0)
            if(conf.isUseAdaGrad())
                gradient.subi(params.mul(conf.getL2()));



        if(conf.isConstrainGradientToUnitNorm())
            gradient.divi(gradient.norm2(Integer.MAX_VALUE));


        gradient.divi(batchSize);


    }


}
