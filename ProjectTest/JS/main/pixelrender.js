// pixelrender/DomUtil.js

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


// pixelrender/Pool.js

/**
 * Pool is the cache pool of the proton engine, it is very important.
 *
 * get(target, params, uid)
 *  Class
 *    uid = Puid.getId -> Puid save target cache
 *    target.__puid = uid
 *
 *  body
 *    uid = Puid.getId -> Puid save target cache
 *
 *
 * expire(target)
 *  cache[target.__puid] push target
 *
 */
import Util from "./Util";
import Puid from "./Puid";

export default class Pool {
  /**
   * @memberof! Proton#
   * @constructor
   * @alias Proton.Pool
   *
   * @todo add description
   * @todo add description of properties
   *
   * @property {Number} total
   * @property {Object} cache
   */
  constructor(num) {
    this.total = 0;
    this.cache = {};
  }

  /**
   * @todo add description
   *
   * @method get
   * @memberof Proton#Proton.Pool
   *
   * @param {Object|Function} target
   * @param {Object} [params] just add if `target` is a function
   *
   * @return {Object}
   */
  get(target, params, uid) {
    let p;
    uid = uid || target.__puid || Puid.getId(target);

    if (this.cache[uid] && this.cache[uid].length > 0) {
      p = this.cache[uid].pop();
    } else {
      p = this.createOrClone(target, params);
    }

    p.__puid = target.__puid || uid;
    return p;
  }

  /**
   * @todo add description
   *
   * @method set
   * @memberof Proton#Proton.Pool
   *
   * @param {Object} target
   *
   * @return {Object}
   */
  expire(target) {
    return this.getCache(target.__puid).push(target);
  }

  /**
   * Creates a new class instance
   *
   * @todo add more documentation
   *
   * @method create
   * @memberof Proton#Proton.Pool
   *
   * @param {Object|Function} target any Object or Function
   * @param {Object} [params] just add if `target` is a function
   *
   * @return {Object}
   */
  createOrClone(target, params) {
    this.total++;

    if (this.create) {
      return this.create(target, params);
    } else if (typeof target === "function") {
      return Util.classApply(target, params);
    } else {
      return target.clone();
    }
  }

  /**
   * @todo add description - what is in the cache?
   *
   * @method getCount
   * @memberof Proton#Proton.Pool
   *
   * @return {Number}
   */
  getCount() {
    let count = 0;
    for (let id in this.cache) count += this.cache[id].length;
    return count++;
  }

  /**
   * Destroyes all items from Pool.cache
   *
   * @method destroy
   * @memberof Proton#Proton.Pool
   */
  destroy() {
    for (let id in this.cache) {
      this.cache[id].length = 0;
      delete this.cache[id];
    }
  }

  /**
   * Returns Pool.cache
   *
   * @method getCache
   * @memberof Proton#Proton.Pool
   * @private
   *
   * @param {Number} uid the unique id
   *
   * @return {Object}
   */
  getCache(uid = "default") {
    if (!this.cache[uid]) this.cache[uid] = [];
    return this.cache[uid];
  }
}


// pixelrender/ImgUtil.js

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


// pixelrender/Util.js

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


// pixelrender/Rectangle.js

export default class Rectangle {
  constructor(x, y, w, h) {
    this.x = x;
    this.y = y;

    this.width = w;
    this.height = h;

    this.bottom = this.y + this.height;
    this.right = this.x + this.width;
  }

  contains(x, y) {
    if (x <= this.right && x >= this.x && y <= this.bottom && y >= this.y) return true;
    else return false;
  }
}


// pixelrender/WebGLUtil.js

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


// pixelrender/Puid.js

const idsMap = {};

const Puid = {
  _index: 0,
  _cache: {},

  id(type) {
    if (idsMap[type] === undefined || idsMap[type] === null) idsMap[type] = 0;
    return `${type}_${idsMap[type]++}`;
  },

  getId(target) {
    let uid = this.getIdFromCache(target);
    if (uid) return uid;

    uid = `PUID_${this._index++}`;
    this._cache[uid] = target;
    return uid;
  },

  getIdFromCache(target) {
    let obj, id;

    for (id in this._cache) {
      obj = this._cache[id];

      if (obj === target) return id;
      if (this.isBody(obj, target) && obj.src === target.src) return id;
    }

    return null;
  },

  isBody(obj, target) {
    return typeof obj === "object" && typeof target === "object" && obj.isInner && target.isInner;
  },

  getTarget(uid) {
    return this._cache[uid];
  }
};

export default Puid;


// pixelrender/PixelRenderer.js

import Rectangle from "./Rectangle";
import BaseRenderer from "./BaseRenderer";

export default class PixelRenderer extends BaseRenderer {
  constructor(element, rectangle) {
    super(element);

    this.context = this.element.getContext("2d");
    this.imageData = null;
    this.rectangle = rectangle;
    this.createImageData(rectangle);

    this.name = "PixelRenderer";
  }

  resize(width, height) {
    this.element.width = width;
    this.element.height = height;
  }

  createImageData(rectangle) {
    this.rectangle = rectangle ? rectangle : new Rectangle(0, 0, this.element.width, this.element.height);
    this.imageData = this.context.createImageData(this.rectangle.width, this.rectangle.height);
    this.context.putImageData(this.imageData, this.rectangle.x, this.rectangle.y);
  }

  onProtonUpdate() {
    this.context.clearRect(this.rectangle.x, this.rectangle.y, this.rectangle.width, this.rectangle.height);
    this.imageData = this.context.getImageData(
      this.rectangle.x,
      this.rectangle.y,
      this.rectangle.width,
      this.rectangle.height
    );
  }

  onProtonUpdateAfter() {
    this.context.putImageData(this.imageData, this.rectangle.x, this.rectangle.y);
  }

  onParticleCreated(particle) {}

  onParticleUpdate(particle) {
    if (this.imageData) {
      this.setPixel(
        this.imageData,
        (particle.p.x - this.rectangle.x) >> 0,
        (particle.p.y - this.rectangle.y) >> 0,
        particle
      );
    }
  }

  setPixel(imagedata, x, y, particle) {
    const rgb = particle.rgb;
    if (x < 0 || x > this.element.width || y < 0 || y > this.element.height) return;

    const i = ((y >> 0) * imagedata.width + (x >> 0)) * 4;
    imagedata.data[i] = rgb.r;
    imagedata.data[i + 1] = rgb.g;
    imagedata.data[i + 2] = rgb.b;
    imagedata.data[i + 3] = particle.alpha * 255;
  }

  onParticleDead(particle) {}

  destroy() {
    super.destroy();
    this.stroke = null;
    this.context = null;
    this.imageData = null;
    this.rectangle = null;
  }
}


// pixelrender/BaseRenderer.js

import Pool from "./Pool";

export default class BaseRenderer {
  constructor(element, stroke) {
    this.pool = new Pool();
    this.element = element;
    this.stroke = stroke;
    this.circleConf = { isCircle: true };

    this.initEventHandler();
    this.name = "BaseRenderer";
  }

  setStroke(color = "#000000", thinkness = 1) {
    this.stroke = { color, thinkness };
  }

  initEventHandler() {
    this._protonUpdateHandler = () => {
      this.onProtonUpdate.call(this);
    };

    this._protonUpdateAfterHandler = () => {
      this.onProtonUpdateAfter.call(this);
    };

    this._emitterAddedHandler = emitter => {
      this.onEmitterAdded.call(this, emitter);
    };

    this._emitterRemovedHandler = emitter => {
      this.onEmitterRemoved.call(this, emitter);
    };

    this._particleCreatedHandler = particle => {
      this.onParticleCreated.call(this, particle);
    };

    this._particleUpdateHandler = particle => {
      this.onParticleUpdate.call(this, particle);
    };

    this._particleDeadHandler = particle => {
      this.onParticleDead.call(this, particle);
    };
  }

  init(proton) {
    this.parent = proton;

    proton.addEventListener("PROTON_UPDATE", this._protonUpdateHandler);
    proton.addEventListener("PROTON_UPDATE_AFTER", this._protonUpdateAfterHandler);

    proton.addEventListener("EMITTER_ADDED", this._emitterAddedHandler);
    proton.addEventListener("EMITTER_REMOVED", this._emitterRemovedHandler);

    proton.addEventListener("PARTICLE_CREATED", this._particleCreatedHandler);
    proton.addEventListener("PARTICLE_UPDATE", this._particleUpdateHandler);
    proton.addEventListener("PARTICLE_DEAD", this._particleDeadHandler);
  }

  resize(width, height) {}

  destroy() {
    this.remove();
    this.pool.destroy();
    this.pool = null;
    this.element = null;
    this.stroke = null;
  }

  remove(proton) {
    this.parent.removeEventListener("PROTON_UPDATE", this._protonUpdateHandler);
    this.parent.removeEventListener("PROTON_UPDATE_AFTER", this._protonUpdateAfterHandler);

    this.parent.removeEventListener("EMITTER_ADDED", this._emitterAddedHandler);
    this.parent.removeEventListener("EMITTER_REMOVED", this._emitterRemovedHandler);

    this.parent.removeEventListener("PARTICLE_CREATED", this._particleCreatedHandler);
    this.parent.removeEventListener("PARTICLE_UPDATE", this._particleUpdateHandler);
    this.parent.removeEventListener("PARTICLE_DEAD", this._particleDeadHandler);

    this.parent = null;
  }

  onProtonUpdate() {}
  onProtonUpdateAfter() {}

  onEmitterAdded(emitter) {}
  onEmitterRemoved(emitter) {}

  onParticleCreated(particle) {}
  onParticleUpdate(particle) {}
  onParticleDead(particle) {}
}


