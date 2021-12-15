/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.mathutils;

/**
 *
 * @author Daniel van Kraalingen
 */
public enum InterpolatorExtrapolationType {
    /** do not allow extrapolation */
    NOEXTRAPOLATION,
    /** allow extrapolation, but use nearest boundary value */
    CONSTANTEXTRAPOLATION,
    /** allow extrapolation, but use slope of last two points */
    SLOPEEXTRAPOLATION
}
