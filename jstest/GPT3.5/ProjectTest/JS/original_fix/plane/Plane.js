var Shape =  require('./Shape')
,    vec2 =  require('./vec2')
,    Utils = require('./Utils');

module.exports = Plane;

/**
 * Plane shape class. The plane is facing in the Y direction.
 * @class Plane
 * @extends Shape
 * @constructor
 * @param {object} [options] (Note that this options object will be passed on to the {{#crossLink "Shape"}}{{/crossLink}} constructor.)
 * @example
 *     var body = new Body();
 *     var shape = new Plane();
 *     body.addShape(shape);
 */
function Plane(options){
    options = options ? Utils.shallowClone(options) : {};
    options.type = Shape.PLANE;
    Shape.call(this, options);
}
Plane.prototype = new Shape();
Plane.prototype.constructor = Plane;

/**
 * Compute moment of inertia
 * @method computeMomentOfInertia
 */
Plane.prototype.computeMomentOfInertia = function(){
    return 0; // Plane is infinite. The inertia should therefore be infinty but by convention we set 0 here
};

/**
 * Update the bounding radius
 * @method updateBoundingRadius
 */
Plane.prototype.updateBoundingRadius = function(){
    this.boundingRadius = Number.MAX_VALUE;
};

/**
 * @method computeAABB
 * @param  {AABB}   out
 * @param  {Array}  position
 * @param  {Number} angle
 */
Plane.prototype.computeAABB = function(out, position, angle){
    var a = angle % (2 * Math.PI);
    var set = vec2.set;
    var max = 1e7;
    var lowerBound = out.lowerBound;
    var upperBound = out.upperBound;

    // Set max bounds
    set(lowerBound, -max, -max);
    set(upperBound,  max,  max);

    if(a === 0){
        // y goes from -inf to 0
        upperBound[1] = position[1];

    } else if(a === Math.PI / 2){

        // x goes from 0 to inf
        lowerBound[0] = position[0];

    } else if(a === Math.PI){

        // y goes from 0 to inf
        lowerBound[1] = position[1];

    } else if(a === 3*Math.PI/2){

        // x goes from -inf to 0
        upperBound[0] = position[0];

    }
};

Plane.prototype.updateArea = function(){
    this.area = Number.MAX_VALUE;
};

var intersectPlane_planePointToFrom = vec2.create();
var intersectPlane_normal = vec2.create();
var intersectPlane_len = vec2.create();

/**
 * @method raycast
 * @param  {RayResult} result
 * @param  {Ray} ray
 * @param  {array} position
 * @param  {number} angle
 */
Plane.prototype.raycast = function(result, ray, position, angle){
    var from = ray.from;
    var to = ray.to;
    var direction = ray.direction;
    var planePointToFrom = intersectPlane_planePointToFrom;
    var normal = intersectPlane_normal;
    var len = intersectPlane_len;

    // Get plane normal
    vec2.set(normal, 0, 1);
    vec2.rotate(normal, normal, angle);

    vec2.subtract(len, from, position);
    var planeToFrom = vec2.dot(len, normal);
    vec2.subtract(len, to, position);
    var planeToTo = vec2.dot(len, normal);

    if(planeToFrom * planeToTo > 0){
        // "from" and "to" are on the same side of the plane... bail out
        return;
    }

    if(vec2.squaredDistance(from, to) < planeToFrom * planeToFrom){
        return;
    }

    var n_dot_dir = vec2.dot(normal, direction);

    vec2.subtract(planePointToFrom, from, position);
    var t = -vec2.dot(normal, planePointToFrom) / n_dot_dir / ray.length;

    ray.reportIntersection(result, t, normal, -1);
};

Plane.prototype.pointTest = function(localPoint){
    return localPoint[1] <= 0;
};
