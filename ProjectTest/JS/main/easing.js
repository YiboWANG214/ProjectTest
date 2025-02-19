// easing/BezierEasing.js

/**
 * @author pschroen / https://ufo.ai/
 *
 * Based on https://github.com/gre/bezier-easing
 */

// These values are established by empiricism with tests (tradeoff: performance VS precision)
const NEWTON_ITERATIONS = 4;
const NEWTON_MIN_SLOPE = 0.001;
const SUBDIVISION_PRECISION = 0.0000001;
const SUBDIVISION_MAX_ITERATIONS = 10;

const kSplineTableSize = 11;
const kSampleStepSize = 1 / (kSplineTableSize - 1);

function A(aA1, aA2) {
    return 1 - 3 * aA2 + 3 * aA1;
}

function B(aA1, aA2) {
    return 3 * aA2 - 6 * aA1;
}

function C(aA1) {
    return 3 * aA1;
}

// Returns x(t) given t, x1, and x2, or y(t) given t, y1, and y2
function calcBezier(aT, aA1, aA2) {
    return ((A(aA1, aA2) * aT + B(aA1, aA2)) * aT + C(aA1)) * aT;
}

// Returns dx/dt given t, x1, and x2, or dy/dt given t, y1, and y2
function getSlope(aT, aA1, aA2) {
    return 3 * A(aA1, aA2) * aT * aT + 2 * B(aA1, aA2) * aT + C(aA1);
}

function binarySubdivide(aX, aA, aB, mX1, mX2) {
    let currentX;
    let currentT;
    let i = 0;

    do {
        currentT = aA + (aB - aA) / 2;
        currentX = calcBezier(currentT, mX1, mX2) - aX;

        if (currentX > 0) {
            aB = currentT;
        } else {
            aA = currentT;
        }
    } while (Math.abs(currentX) > SUBDIVISION_PRECISION && ++i < SUBDIVISION_MAX_ITERATIONS);

    return currentT;
}

function newtonRaphsonIterate(aX, aGuessT, mX1, mX2) {
    for (let i = 0; i < NEWTON_ITERATIONS; i++) {
        const currentSlope = getSlope(aGuessT, mX1, mX2);

        if (currentSlope === 0) {
            return aGuessT;
        }

        const currentX = calcBezier(aGuessT, mX1, mX2) - aX;
        aGuessT -= currentX / currentSlope;
    }

    return aGuessT;
}

function LinearEasing(x) {
    return x;
}

export default function bezier(mX1, mY1, mX2, mY2) {
    if (!(0 <= mX1 && mX1 <= 1 && 0 <= mX2 && mX2 <= 1)) {
        throw new Error('Bezier x values must be in [0, 1] range');
    }

    if (mX1 === mY1 && mX2 === mY2) {
        return LinearEasing;
    }

    // Precompute samples table
    const sampleValues = new Float32Array(kSplineTableSize);
    for (let i = 0; i < kSplineTableSize; i++) {
        sampleValues[i] = calcBezier(i * kSampleStepSize, mX1, mX2);
    }

    function getTForX(aX) {
        let intervalStart = 0;
        let currentSample = 1;
        const lastSample = kSplineTableSize - 1;

        for (; currentSample !== lastSample && sampleValues[currentSample] <= aX; currentSample++) {
            intervalStart += kSampleStepSize;
        }
        currentSample--;

        // Interpolate to provide an initial guess for t
        const dist = (aX - sampleValues[currentSample]) / (sampleValues[currentSample + 1] - sampleValues[currentSample]);
        const guessForT = intervalStart + dist * kSampleStepSize;

        const initialSlope = getSlope(guessForT, mX1, mX2);
        if (initialSlope >= NEWTON_MIN_SLOPE) {
            return newtonRaphsonIterate(aX, guessForT, mX1, mX2);
        } else if (initialSlope === 0) {
            return guessForT;
        } else {
            return binarySubdivide(aX, intervalStart, intervalStart + kSampleStepSize, mX1, mX2);
        }
    }

    return function BezierEasing(x) {
        // Because JavaScript numbers are imprecise, we should guarantee the extremes are right
        if (x === 0 || x === 1) {
            return x;
        }

        return calcBezier(getTForX(x), mY1, mY2);
    };
}


// easing/Easing.js

/**
 * @author pschroen / https://ufo.ai/
 *
 * Based on https://github.com/danro/easing-js
 * Based on https://github.com/CreateJS/TweenJS
 * Based on https://github.com/tweenjs/tween.js
 * Based on https://easings.net/
 */

import BezierEasing from './BezierEasing.js';

export class Easing {
    static linear(t) {
        return t;
    }

    static easeInQuad(t) {
        return t * t;
    }

    static easeOutQuad(t) {
        return t * (2 - t);
    }

    static easeInOutQuad(t) {
        if ((t *= 2) < 1) {
            return 0.5 * t * t;
        }

        return -0.5 * (--t * (t - 2) - 1);
    }

    static easeInCubic(t) {
        return t * t * t;
    }

    static easeOutCubic(t) {
        return --t * t * t + 1;
    }

    static easeInOutCubic(t) {
        if ((t *= 2) < 1) {
            return 0.5 * t * t * t;
        }

        return 0.5 * ((t -= 2) * t * t + 2);
    }

    static easeInQuart(t) {
        return t * t * t * t;
    }

    static easeOutQuart(t) {
        return 1 - --t * t * t * t;
    }

    static easeInOutQuart(t) {
        if ((t *= 2) < 1) {
            return 0.5 * t * t * t * t;
        }

        return -0.5 * ((t -= 2) * t * t * t - 2);
    }

    static easeInQuint(t) {
        return t * t * t * t * t;
    }

    static easeOutQuint(t) {
        return --t * t * t * t * t + 1;
    }

    static easeInOutQuint(t) {
        if ((t *= 2) < 1) {
            return 0.5 * t * t * t * t * t;
        }

        return 0.5 * ((t -= 2) * t * t * t * t + 2);
    }

    static easeInSine(t) {
        return 1 - Math.sin(((1 - t) * Math.PI) / 2);
    }

    static easeOutSine(t) {
        return Math.sin((t * Math.PI) / 2);
    }

    static easeInOutSine(t) {
        return 0.5 * (1 - Math.sin(Math.PI * (0.5 - t)));
    }

    static easeInExpo(t) {
        return t === 0 ? 0 : Math.pow(1024, t - 1);
    }

    static easeOutExpo(t) {
        return t === 1 ? 1 : 1 - Math.pow(2, -10 * t);
    }

    static easeInOutExpo(t) {
        if (t === 0 || t === 1) {
            return t;
        }

        if ((t *= 2) < 1) {
            return 0.5 * Math.pow(1024, t - 1);
        }

        return 0.5 * (-Math.pow(2, -10 * (t - 1)) + 2);
    }

    static easeInCirc(t) {
        return 1 - Math.sqrt(1 - t * t);
    }

    static easeOutCirc(t) {
        return Math.sqrt(1 - --t * t);
    }

    static easeInOutCirc(t) {
        if ((t *= 2) < 1) {
            return -0.5 * (Math.sqrt(1 - t * t) - 1);
        }

        return 0.5 * (Math.sqrt(1 - (t -= 2) * t) + 1);
    }

    static easeInBack(t) {
        const s = 1.70158;

        return t === 1 ? 1 : t * t * ((s + 1) * t - s);
    }

    static easeOutBack(t) {
        const s = 1.70158;

        return t === 0 ? 0 : --t * t * ((s + 1) * t + s) + 1;
    }

    static easeInOutBack(t) {
        const s = 1.70158 * 1.525;

        if ((t *= 2) < 1) {
            return 0.5 * (t * t * ((s + 1) * t - s));
        }

        return 0.5 * ((t -= 2) * t * ((s + 1) * t + s) + 2);
    }

    static easeInElastic(t, amplitude = 1, period = 0.3) {
        if (t === 0 || t === 1) {
            return t;
        }

        const pi2 = Math.PI * 2;
        const s = period / pi2 * Math.asin(1 / amplitude);

        return -(amplitude * Math.pow(2, 10 * --t) * Math.sin((t - s) * pi2 / period));
    }

    static easeOutElastic(t, amplitude = 1, period = 0.3) {
        if (t === 0 || t === 1) {
            return t;
        }

        const pi2 = Math.PI * 2;
        const s = period / pi2 * Math.asin(1 / amplitude);

        return amplitude * Math.pow(2, -10 * t) * Math.sin((t - s) * pi2 / period) + 1;
    }

    static easeInOutElastic(t, amplitude = 1, period = 0.3 * 1.5) {
        if (t === 0 || t === 1) {
            return t;
        }

        const pi2 = Math.PI * 2;
        const s = period / pi2 * Math.asin(1 / amplitude);

        if ((t *= 2) < 1) {
            return -0.5 * (amplitude * Math.pow(2, 10 * --t) * Math.sin((t - s) * pi2 / period));
        }

        return amplitude * Math.pow(2, -10 * --t) * Math.sin((t - s) * pi2 / period) * 0.5 + 1;
    }

    static easeInBounce(t) {
        return 1 - this.easeOutBounce(1 - t);
    }

    static easeOutBounce(t) {
        const n1 = 7.5625;
        const d1 = 2.75;

        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            return n1 * (t -= 1.5 / d1) * t + 0.75;
        } else if (t < 2.5 / d1) {
            return n1 * (t -= 2.25 / d1) * t + 0.9375;
        } else {
            return n1 * (t -= 2.625 / d1) * t + 0.984375;
        }
    }

    static easeInOutBounce(t) {
        if (t < 0.5) {
            return this.easeInBounce(t * 2) * 0.5;
        }

        return this.easeOutBounce(t * 2 - 1) * 0.5 + 0.5;
    }

    static addBezier(name, mX1, mY1, mX2, mY2) {
        this[name] = BezierEasing(mX1, mY1, mX2, mY2);
    }
}


