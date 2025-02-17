// span/Span.js

import Util from "./Util";
import MathUtil from "./MathUtil";

export default class Span {
  constructor(a, b, center) {
    if (Util.isArray(a)) {
      this.isArray = true;
      this.a = a;
    } else {
      this.isArray = false;
      this.a = Util.initValue(a, 1);
      this.b = Util.initValue(b, this.a);
      this.center = Util.initValue(center, false);
    }
  }

  getValue(isInt = false) {
    if (this.isArray) {
      return Util.getRandFromArray(this.a);
    } else {
      if (!this.center) {
        return MathUtil.randomAToB(this.a, this.b, isInt);
      } else {
        return MathUtil.randomFloating(this.a, this.b, isInt);
      }
    }
  }

  /**
   * Returns a new Span object
   *
   * @memberof Proton#Proton.Util
   * @method setSpanValue
   *
   * @todo a, b and c should be 'Mixed' or 'Number'?
   *
   * @param {Mixed | Span} a
   * @param {Mixed}               b
   * @param {Mixed}               c
   *
   * @return {Span}
   */
  static setSpanValue(a, b, c) {
    if (a instanceof Span) {
      return a;
    } else {
      if (b === undefined) {
        return new Span(a);
      } else {
        if (c === undefined) return new Span(a, b);
        else return new Span(a, b, c);
      }
    }
  }

  /**
   * Returns the value from a Span, if the param is not a Span it will return the given parameter
   *
   * @memberof Proton#Proton.Util
   * @method getValue
   *
   * @param {Mixed | Span} pan
   *
   * @return {Mixed} the value of Span OR the parameter if it is not a Span
   */
  static getSpanValue(pan) {
    return pan instanceof Span ? pan.getValue() : pan;
  }
}


// span/DomUtil.js

export default {
  /**
   * Creates and returns a new canvas. The opacity is by default set to 0
   *
   * @memberof Proton#Proton.DomUtil
   * @method createCanvas
   *
   * @param {String} $id the canvas' id
   * @param {Number} $width the canvas' width
   * @param {Number} $height the canvas' height
   * @param {String} [$position=absolute] the canvas' position, default is 'absolute'
   *
   * @return {Object}
   */
  createCanvas(id, width, height, position = "absolute") {
    const dom = document.createElement("canvas");

    dom.id = id;
    dom.width = width;
    dom.height = height;
    dom.style.opacity = 0;
    dom.style.position = position;
    this.transform(dom, -500, -500, 0, 0);

    return dom;
  },

  createDiv(id, width, height) {
    const dom = document.createElement("div");

    dom.id = id;
    dom.style.position = "absolute";
    this.resize(dom, width, height);

    return dom;
  },

  resize(dom, width, height) {
    dom.style.width = width + "px";
    dom.style.height = height + "px";
    dom.style.marginLeft = -width / 2 + "px";
    dom.style.marginTop = -height / 2 + "px";
  },

  /**
   * Adds a transform: translate(), scale(), rotate() to a given div dom for all browsers
   *
   * @memberof Proton#Proton.DomUtil
   * @method transform
   *
   * @param {HTMLDivElement} div
   * @param {Number} $x
   * @param {Number} $y
   * @param {Number} $scale
   * @param {Number} $rotate
   */
  transform(div, x, y, scale, rotate) {
    div.style.willChange = "transform";
    const transform = `translate(${x}px, ${y}px) scale(${scale}) rotate(${rotate}deg)`;
    this.css3(div, "transform", transform);
  },

  transform3d(div, x, y, scale, rotate) {
    div.style.willChange = "transform";
    const transform = `translate3d(${x}px, ${y}px, 0) scale(${scale}) rotate(${rotate}deg)`;
    this.css3(div, "backfaceVisibility", "hidden");
    this.css3(div, "transform", transform);
  },

  css3(div, key, val) {
    const bkey = key.charAt(0).toUpperCase() + key.substr(1);

    div.style[`Webkit${bkey}`] = val;
    div.style[`Moz${bkey}`] = val;
    div.style[`O${bkey}`] = val;
    div.style[`ms${bkey}`] = val;
    div.style[`${key}`] = val;
  }
};


// span/ImgUtil.js

import WebGLUtil from "./WebGLUtil";
import DomUtil from "./DomUtil";

const imgsCache = {};
const canvasCache = {};
let canvasId = 0;

export default {
  /**
   * This will get the image data. It could be necessary to create a Proton.Zone.
   *
   * @memberof Proton#Proton.Util
   * @method getImageData
   *
   * @param {HTMLCanvasElement}   context any canvas, must be a 2dContext 'canvas.getContext('2d')'
   * @param {Object}              image   could be any dom image, e.g. document.getElementById('thisIsAnImgTag');
   * @param {Proton.Rectangle}    rect
   */
  getImageData(context, image, rect) {
    context.drawImage(image, rect.x, rect.y);
    const imagedata = context.getImageData(rect.x, rect.y, rect.width, rect.height);
    context.clearRect(rect.x, rect.y, rect.width, rect.height);

    return imagedata;
  },

  /**
   * @memberof Proton#Proton.Util
   * @method getImgFromCache
   *
   * @todo add description
   * @todo describe func
   *
   * @param {Mixed}               img
   * @param {Proton.Particle}     particle
   * @param {Boolean}             drawCanvas  set to true if a canvas should be saved into particle.data.canvas
   * @param {Boolean}             func
   */
  getImgFromCache(img, callback, param) {
    const src = typeof img === "string" ? img : img.src;

    if (imgsCache[src]) {
      callback(imgsCache[src], param);
    } else {
      const image = new Image();
      image.onload = e => {
        imgsCache[src] = e.target;
        callback(imgsCache[src], param);
      };

      image.src = src;
    }
  },

  getCanvasFromCache(img, callback, param) {
    const src = img.src;

    if (!canvasCache[src]) {
      const width = WebGLUtil.nhpot(img.width);
      const height = WebGLUtil.nhpot(img.height);

      const canvas = DomUtil.createCanvas(`proton_canvas_cache_${++canvasId}`, width, height);
      const context = canvas.getContext("2d");
      context.drawImage(img, 0, 0, img.width, img.height);

      canvasCache[src] = canvas;
    }

    callback && callback(canvasCache[src], param);

    return canvasCache[src];
  }
};


// span/Util.js

import ImgUtil from "./ImgUtil";

export default {
  /**
   * Returns the default if the value is null or undefined
   *
   * @memberof Proton#Proton.Util
   * @method initValue
   *
   * @param {Mixed} value a specific value, could be everything but null or undefined
   * @param {Mixed} defaults the default if the value is null or undefined
   */
  initValue(value, defaults) {
    value = value !== null && value !== undefined ? value : defaults;
    return value;
  },

  /**
   * Checks if the value is a valid array
   *
   * @memberof Proton#Proton.Util
   * @method isArray
   *
   * @param {Array} value Any array
   *
   * @returns {Boolean}
   */
  isArray(value) {
    return Object.prototype.toString.call(value) === "[object Array]";
  },

  /**
   * Destroyes the given array
   *
   * @memberof Proton#Proton.Util
   * @method emptyArray
   *
   * @param {Array} array Any array
   */
  emptyArray(arr) {
    if (arr) arr.length = 0;
  },

  toArray(arr) {
    return this.isArray(arr) ? arr : [arr];
  },

  sliceArray(arr1, index, arr2) {
    this.emptyArray(arr2);
    for (let i = index; i < arr1.length; i++) {
      arr2.push(arr1[i]);
    }
  },

  getRandFromArray(arr) {
    if (!arr) return null;
    return arr[Math.floor(arr.length * Math.random())];
  },

  /**
   * Destroyes the given object
   *
   * @memberof Proton#Proton.Util
   * @method emptyObject
   *
   * @param {Object} obj Any object
   */
  emptyObject(obj, ignore = null) {
    for (let key in obj) {
      if (ignore && ignore.indexOf(key) > -1) continue;
      delete obj[key];
    }
  },

  /**
   * Makes an instance of a class and binds the given array
   *
   * @memberof Proton#Proton.Util
   * @method classApply
   *
   * @param {Function} constructor A class to make an instance from
   * @param {Array} [args] Any array to bind it to the constructor
   *
   * @return {Object} The instance of constructor, optionally bind with args
   */
  classApply(constructor, args = null) {
    if (!args) {
      return new constructor();
    } else {
      const FactoryFunc = constructor.bind.apply(constructor, [null].concat(args));
      return new FactoryFunc();
    }
  },

  /**
   * This will get the image data. It could be necessary to create a Proton.Zone.
   *
   * @memberof Proton#Proton.Util
   * @method getImageData
   *
   * @param {HTMLCanvasElement}   context any canvas, must be a 2dContext 'canvas.getContext('2d')'
   * @param {Object}              image   could be any dom image, e.g. document.getElementById('thisIsAnImgTag');
   * @param {Proton.Rectangle}    rect
   */
  getImageData(context, image, rect) {
    return ImgUtil.getImageData(context, image, rect);
  },

  destroyAll(arr, param = null) {
    let i = arr.length;

    while (i--) {
      try {
        arr[i].destroy(param);
      } catch (e) {}

      delete arr[i];
    }

    arr.length = 0;
  },

  assign(target, source) {
    if (typeof Object.assign !== "function") {
      for (let key in source) {
        if (Object.prototype.hasOwnProperty.call(source, key)) {
          target[key] = source[key];
        }
      }

      return target;
    } else {
      return Object.assign(target, source);
    }
  }
};


// span/WebGLUtil.js

export default {
  /**
   * @memberof Proton#Proton.WebGLUtil
   * @method ipot
   *
   * @todo add description
   * @todo add length description
   *
   * @param {Number} length
   *
   * @return {Boolean}
   */
  ipot(length) {
    return (length & (length - 1)) === 0;
  },

  /**
   * @memberof Proton#Proton.WebGLUtil
   * @method nhpot
   *
   * @todo add description
   * @todo add length description
   *
   * @param {Number} length
   *
   * @return {Number}
   */
  nhpot(length) {
    --length;
    for (let i = 1; i < 32; i <<= 1) {
      length = length | (length >> i);
    }

    return length + 1;
  },

  /**
   * @memberof Proton#Proton.WebGLUtil
   * @method makeTranslation
   *
   * @todo add description
   * @todo add tx, ty description
   * @todo add return description
   *
   * @param {Number} tx either 0 or 1
   * @param {Number} ty either 0 or 1
   *
   * @return {Object}
   */
  makeTranslation(tx, ty) {
    return [1, 0, 0, 0, 1, 0, tx, ty, 1];
  },

  /**
   * @memberof Proton#Proton.WebGLUtil
   * @method makeRotation
   *
   * @todo add description
   * @todo add return description
   *
   * @param {Number} angleInRadians
   *
   * @return {Object}
   */
  makeRotation(angleInRadians) {
    let c = Math.cos(angleInRadians);
    let s = Math.sin(angleInRadians);

    return [c, -s, 0, s, c, 0, 0, 0, 1];
  },

  /**
   * @memberof Proton#Proton.WebGLUtil
   * @method makeScale
   *
   * @todo add description
   * @todo add tx, ty description
   * @todo add return description
   *
   * @param {Number} sx either 0 or 1
   * @param {Number} sy either 0 or 1
   *
   * @return {Object}
   */
  makeScale(sx, sy) {
    return [sx, 0, 0, 0, sy, 0, 0, 0, 1];
  },

  /**
   * @memberof Proton#Proton.WebGLUtil
   * @method matrixMultiply
   *
   * @todo add description
   * @todo add a, b description
   * @todo add return description
   *
   * @param {Object} a
   * @param {Object} b
   *
   * @return {Object}
   */
  matrixMultiply(a, b) {
    let a00 = a[0 * 3 + 0];
    let a01 = a[0 * 3 + 1];
    let a02 = a[0 * 3 + 2];
    let a10 = a[1 * 3 + 0];
    let a11 = a[1 * 3 + 1];
    let a12 = a[1 * 3 + 2];
    let a20 = a[2 * 3 + 0];
    let a21 = a[2 * 3 + 1];
    let a22 = a[2 * 3 + 2];
    let b00 = b[0 * 3 + 0];
    let b01 = b[0 * 3 + 1];
    let b02 = b[0 * 3 + 2];
    let b10 = b[1 * 3 + 0];
    let b11 = b[1 * 3 + 1];
    let b12 = b[1 * 3 + 2];
    let b20 = b[2 * 3 + 0];
    let b21 = b[2 * 3 + 1];
    let b22 = b[2 * 3 + 2];

    return [
      a00 * b00 + a01 * b10 + a02 * b20,
      a00 * b01 + a01 * b11 + a02 * b21,
      a00 * b02 + a01 * b12 + a02 * b22,
      a10 * b00 + a11 * b10 + a12 * b20,
      a10 * b01 + a11 * b11 + a12 * b21,
      a10 * b02 + a11 * b12 + a12 * b22,
      a20 * b00 + a21 * b10 + a22 * b20,
      a20 * b01 + a21 * b11 + a22 * b21,
      a20 * b02 + a21 * b12 + a22 * b22
    ];
  }
};


// span/MathUtil.js

const PI = 3.1415926;
const INFINITY = Infinity;

const MathUtil = {
  PI: PI,
  PIx2: PI * 2,
  PI_2: PI / 2,
  PI_180: PI / 180,
  N180_PI: 180 / PI,
  Infinity: -999,

  isInfinity(num) {
    return num === this.Infinity || num === INFINITY;
  },

  randomAToB(a, b, isInt = false) {
    if (!isInt) return a + Math.random() * (b - a);
    else return ((Math.random() * (b - a)) >> 0) + a;
  },

  randomFloating(center, f, isInt) {
    return this.randomAToB(center - f, center + f, isInt);
  },

  randomColor() {
    return "#" + ("00000" + ((Math.random() * 0x1000000) << 0).toString(16)).slice(-6);
  },

  randomZone(display) {},

  floor(num, k = 4) {
    const digits = Math.pow(10, k);
    return Math.floor(num * digits) / digits;
  },

  degreeTransform(a) {
    return (a * PI) / 180;
  },

  toColor16(num) {
    return `#${num.toString(16)}`;
  }
};

export default MathUtil;


