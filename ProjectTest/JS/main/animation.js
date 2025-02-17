// animation/AnimationAction.js

import { WrapAroundEnding, ZeroCurvatureEnding, ZeroSlopeEnding, LoopPingPong, LoopOnce, LoopRepeat, NormalAnimationBlendMode, AdditiveAnimationBlendMode } from './constants.js';


class AnimationAction {

	constructor( mixer, clip, localRoot = null, blendMode = clip.blendMode ) {

		this._mixer = mixer;
		this._clip = clip;
		this._localRoot = localRoot;
		this.blendMode = blendMode;

		const tracks = clip.tracks,
			nTracks = tracks.length,
			interpolants = new Array( nTracks );

		const interpolantSettings = {
			endingStart: ZeroCurvatureEnding,
			endingEnd: ZeroCurvatureEnding
		};

		for ( let i = 0; i !== nTracks; ++ i ) {

			const interpolant = tracks[ i ].createInterpolant( null );
			interpolants[ i ] = interpolant;
			interpolant.settings = interpolantSettings;

		}

		this._interpolantSettings = interpolantSettings;

		this._interpolants = interpolants; // bound by the mixer

		// inside: PropertyMixer (managed by the mixer)
		this._propertyBindings = new Array( nTracks );

		this._cacheIndex = null; // for the memory manager
		this._byClipCacheIndex = null; // for the memory manager

		this._timeScaleInterpolant = null;
		this._weightInterpolant = null;

		this.loop = LoopRepeat;
		this._loopCount = - 1;

		// global mixer time when the action is to be started
		// it's set back to 'null' upon start of the action
		this._startTime = null;

		// scaled local time of the action
		// gets clamped or wrapped to 0..clip.duration according to loop
		this.time = 0;

		this.timeScale = 1;
		this._effectiveTimeScale = 1;

		this.weight = 1;
		this._effectiveWeight = 1;

		this.repetitions = Infinity; // no. of repetitions when looping

		this.paused = false; // true -> zero effective time scale
		this.enabled = true; // false -> zero effective weight

		this.clampWhenFinished = false;// keep feeding the last frame?

		this.zeroSlopeAtStart = true;// for smooth interpolation w/o separate
		this.zeroSlopeAtEnd = true;// clips for start, loop and end

	}

	// State & Scheduling

	play() {

		this._mixer._activateAction( this );

		return this;

	}

	stop() {

		this._mixer._deactivateAction( this );

		return this.reset();

	}

	reset() {

		this.paused = false;
		this.enabled = true;

		this.time = 0; // restart clip
		this._loopCount = - 1;// forget previous loops
		this._startTime = null;// forget scheduling

		return this.stopFading().stopWarping();

	}

	isRunning() {

		return this.enabled && ! this.paused && this.timeScale !== 0 &&
			this._startTime === null && this._mixer._isActiveAction( this );

	}

	// return true when play has been called
	isScheduled() {

		return this._mixer._isActiveAction( this );

	}

	startAt( time ) {

		this._startTime = time;

		return this;

	}

	setLoop( mode, repetitions ) {

		this.loop = mode;
		this.repetitions = repetitions;

		return this;

	}

	// Weight

	// set the weight stopping any scheduled fading
	// although .enabled = false yields an effective weight of zero, this
	// method does *not* change .enabled, because it would be confusing
	setEffectiveWeight( weight ) {

		this.weight = weight;

		// note: same logic as when updated at runtime
		this._effectiveWeight = this.enabled ? weight : 0;

		return this.stopFading();

	}

	// return the weight considering fading and .enabled
	getEffectiveWeight() {

		return this._effectiveWeight;

	}

	fadeIn( duration ) {

		return this._scheduleFading( duration, 0, 1 );

	}

	fadeOut( duration ) {

		return this._scheduleFading( duration, 1, 0 );

	}

	crossFadeFrom( fadeOutAction, duration, warp ) {

		fadeOutAction.fadeOut( duration );
		this.fadeIn( duration );

		if ( warp ) {

			const fadeInDuration = this._clip.duration,
				fadeOutDuration = fadeOutAction._clip.duration,

				startEndRatio = fadeOutDuration / fadeInDuration,
				endStartRatio = fadeInDuration / fadeOutDuration;

			fadeOutAction.warp( 1.0, startEndRatio, duration );
			this.warp( endStartRatio, 1.0, duration );

		}

		return this;

	}

	crossFadeTo( fadeInAction, duration, warp ) {

		return fadeInAction.crossFadeFrom( this, duration, warp );

	}

	stopFading() {

		const weightInterpolant = this._weightInterpolant;

		if ( weightInterpolant !== null ) {

			this._weightInterpolant = null;
			this._mixer._takeBackControlInterpolant( weightInterpolant );

		}

		return this;

	}

	// Time Scale Control

	// set the time scale stopping any scheduled warping
	// although .paused = true yields an effective time scale of zero, this
	// method does *not* change .paused, because it would be confusing
	setEffectiveTimeScale( timeScale ) {

		this.timeScale = timeScale;
		this._effectiveTimeScale = this.paused ? 0 : timeScale;

		return this.stopWarping();

	}

	// return the time scale considering warping and .paused
	getEffectiveTimeScale() {

		return this._effectiveTimeScale;

	}

	setDuration( duration ) {

		this.timeScale = this._clip.duration / duration;

		return this.stopWarping();

	}

	syncWith( action ) {

		this.time = action.time;
		this.timeScale = action.timeScale;

		return this.stopWarping();

	}

	halt( duration ) {

		return this.warp( this._effectiveTimeScale, 0, duration );

	}

	warp( startTimeScale, endTimeScale, duration ) {

		const mixer = this._mixer,
			now = mixer.time,
			timeScale = this.timeScale;

		let interpolant = this._timeScaleInterpolant;

		if ( interpolant === null ) {

			interpolant = mixer._lendControlInterpolant();
			this._timeScaleInterpolant = interpolant;

		}

		const times = interpolant.parameterPositions,
			values = interpolant.sampleValues;

		times[ 0 ] = now;
		times[ 1 ] = now + duration;

		values[ 0 ] = startTimeScale / timeScale;
		values[ 1 ] = endTimeScale / timeScale;

		return this;

	}

	stopWarping() {

		const timeScaleInterpolant = this._timeScaleInterpolant;

		if ( timeScaleInterpolant !== null ) {

			this._timeScaleInterpolant = null;
			this._mixer._takeBackControlInterpolant( timeScaleInterpolant );

		}

		return this;

	}

	// Object Accessors

	getMixer() {

		return this._mixer;

	}

	getClip() {

		return this._clip;

	}

	getRoot() {

		return this._localRoot || this._mixer._root;

	}

	// Interna

	_update( time, deltaTime, timeDirection, accuIndex ) {

		// called by the mixer

		if ( ! this.enabled ) {

			// call ._updateWeight() to update ._effectiveWeight

			this._updateWeight( time );
			return;

		}

		const startTime = this._startTime;

		if ( startTime !== null ) {

			// check for scheduled start of action

			const timeRunning = ( time - startTime ) * timeDirection;
			if ( timeRunning < 0 || timeDirection === 0 ) {

				deltaTime = 0;

			} else {


				this._startTime = null; // unschedule
				deltaTime = timeDirection * timeRunning;

			}

		}

		// apply time scale and advance time

		deltaTime *= this._updateTimeScale( time );
		const clipTime = this._updateTime( deltaTime );

		// note: _updateTime may disable the action resulting in
		// an effective weight of 0

		const weight = this._updateWeight( time );

		if ( weight > 0 ) {

			const interpolants = this._interpolants;
			const propertyMixers = this._propertyBindings;

			switch ( this.blendMode ) {

				case AdditiveAnimationBlendMode:

					for ( let j = 0, m = interpolants.length; j !== m; ++ j ) {

						interpolants[ j ].evaluate( clipTime );
						propertyMixers[ j ].accumulateAdditive( weight );

					}

					break;

				case NormalAnimationBlendMode:
				default:

					for ( let j = 0, m = interpolants.length; j !== m; ++ j ) {

						interpolants[ j ].evaluate( clipTime );
						propertyMixers[ j ].accumulate( accuIndex, weight );

					}

			}

		}

	}

	_updateWeight( time ) {

		let weight = 0;

		if ( this.enabled ) {

			weight = this.weight;
			const interpolant = this._weightInterpolant;

			if ( interpolant !== null ) {

				const interpolantValue = interpolant.evaluate( time )[ 0 ];

				weight *= interpolantValue;

				if ( time > interpolant.parameterPositions[ 1 ] ) {

					this.stopFading();

					if ( interpolantValue === 0 ) {

						// faded out, disable
						this.enabled = false;

					}

				}

			}

		}

		this._effectiveWeight = weight;
		return weight;

	}

	_updateTimeScale( time ) {

		let timeScale = 0;

		if ( ! this.paused ) {

			timeScale = this.timeScale;

			const interpolant = this._timeScaleInterpolant;

			if ( interpolant !== null ) {

				const interpolantValue = interpolant.evaluate( time )[ 0 ];

				timeScale *= interpolantValue;

				if ( time > interpolant.parameterPositions[ 1 ] ) {

					this.stopWarping();

					if ( timeScale === 0 ) {

						// motion has halted, pause
						this.paused = true;

					} else {

						// warp done - apply final time scale
						this.timeScale = timeScale;

					}

				}

			}

		}

		this._effectiveTimeScale = timeScale;
		return timeScale;

	}

	_updateTime( deltaTime ) {

		const duration = this._clip.duration;
		const loop = this.loop;

		let time = this.time + deltaTime;
		let loopCount = this._loopCount;

		const pingPong = ( loop === LoopPingPong );

		if ( deltaTime === 0 ) {

			if ( loopCount === - 1 ) return time;

			return ( pingPong && ( loopCount & 1 ) === 1 ) ? duration - time : time;

		}

		if ( loop === LoopOnce ) {

			if ( loopCount === - 1 ) {

				// just started

				this._loopCount = 0;
				this._setEndings( true, true, false );

			}

			handle_stop: {

				if ( time >= duration ) {

					time = duration;

				} else if ( time < 0 ) {

					time = 0;

				} else {

					this.time = time;

					break handle_stop;

				}

				if ( this.clampWhenFinished ) this.paused = true;
				else this.enabled = false;

				this.time = time;

				this._mixer.dispatchEvent( {
					type: 'finished', action: this,
					direction: deltaTime < 0 ? - 1 : 1
				} );

			}

		} else { // repetitive Repeat or PingPong

			if ( loopCount === - 1 ) {

				// just started

				if ( deltaTime >= 0 ) {

					loopCount = 0;

					this._setEndings( true, this.repetitions === 0, pingPong );

				} else {

					// when looping in reverse direction, the initial
					// transition through zero counts as a repetition,
					// so leave loopCount at -1

					this._setEndings( this.repetitions === 0, true, pingPong );

				}

			}

			if ( time >= duration || time < 0 ) {

				// wrap around

				const loopDelta = Math.floor( time / duration ); // signed
				time -= duration * loopDelta;

				loopCount += Math.abs( loopDelta );

				const pending = this.repetitions - loopCount;

				if ( pending <= 0 ) {

					// have to stop (switch state, clamp time, fire event)

					if ( this.clampWhenFinished ) this.paused = true;
					else this.enabled = false;

					time = deltaTime > 0 ? duration : 0;

					this.time = time;

					this._mixer.dispatchEvent( {
						type: 'finished', action: this,
						direction: deltaTime > 0 ? 1 : - 1
					} );

				} else {

					// keep running

					if ( pending === 1 ) {

						// entering the last round

						const atStart = deltaTime < 0;
						this._setEndings( atStart, ! atStart, pingPong );

					} else {

						this._setEndings( false, false, pingPong );

					}

					this._loopCount = loopCount;

					this.time = time;

					this._mixer.dispatchEvent( {
						type: 'loop', action: this, loopDelta: loopDelta
					} );

				}

			} else {

				this.time = time;

			}

			if ( pingPong && ( loopCount & 1 ) === 1 ) {

				// invert time for the "pong round"

				return duration - time;

			}

		}

		return time;

	}

	_setEndings( atStart, atEnd, pingPong ) {

		const settings = this._interpolantSettings;

		if ( pingPong ) {

			settings.endingStart = ZeroSlopeEnding;
			settings.endingEnd = ZeroSlopeEnding;

		} else {

			// assuming for LoopOnce atStart == atEnd == true

			if ( atStart ) {

				settings.endingStart = this.zeroSlopeAtStart ? ZeroSlopeEnding : ZeroCurvatureEnding;

			} else {

				settings.endingStart = WrapAroundEnding;

			}

			if ( atEnd ) {

				settings.endingEnd = this.zeroSlopeAtEnd ? ZeroSlopeEnding : ZeroCurvatureEnding;

			} else {

				settings.endingEnd 	 = WrapAroundEnding;

			}

		}

	}

	_scheduleFading( duration, weightNow, weightThen ) {

		const mixer = this._mixer, now = mixer.time;
		let interpolant = this._weightInterpolant;

		if ( interpolant === null ) {

			interpolant = mixer._lendControlInterpolant();
			this._weightInterpolant = interpolant;

		}

		const times = interpolant.parameterPositions,
			values = interpolant.sampleValues;

		times[ 0 ] = now;
		values[ 0 ] = weightNow;
		times[ 1 ] = now + duration;
		values[ 1 ] = weightThen;

		return this;

	}

}


export { AnimationAction };


// animation/constants.js

export const REVISION = '172dev';

export const MOUSE = { LEFT: 0, MIDDLE: 1, RIGHT: 2, ROTATE: 0, DOLLY: 1, PAN: 2 };
export const TOUCH = { ROTATE: 0, PAN: 1, DOLLY_PAN: 2, DOLLY_ROTATE: 3 };
export const CullFaceNone = 0;
export const CullFaceBack = 1;
export const CullFaceFront = 2;
export const CullFaceFrontBack = 3;
export const BasicShadowMap = 0;
export const PCFShadowMap = 1;
export const PCFSoftShadowMap = 2;
export const VSMShadowMap = 3;
export const FrontSide = 0;
export const BackSide = 1;
export const DoubleSide = 2;
export const NoBlending = 0;
export const NormalBlending = 1;
export const AdditiveBlending = 2;
export const SubtractiveBlending = 3;
export const MultiplyBlending = 4;
export const CustomBlending = 5;
export const AddEquation = 100;
export const SubtractEquation = 101;
export const ReverseSubtractEquation = 102;
export const MinEquation = 103;
export const MaxEquation = 104;
export const ZeroFactor = 200;
export const OneFactor = 201;
export const SrcColorFactor = 202;
export const OneMinusSrcColorFactor = 203;
export const SrcAlphaFactor = 204;
export const OneMinusSrcAlphaFactor = 205;
export const DstAlphaFactor = 206;
export const OneMinusDstAlphaFactor = 207;
export const DstColorFactor = 208;
export const OneMinusDstColorFactor = 209;
export const SrcAlphaSaturateFactor = 210;
export const ConstantColorFactor = 211;
export const OneMinusConstantColorFactor = 212;
export const ConstantAlphaFactor = 213;
export const OneMinusConstantAlphaFactor = 214;
export const NeverDepth = 0;
export const AlwaysDepth = 1;
export const LessDepth = 2;
export const LessEqualDepth = 3;
export const EqualDepth = 4;
export const GreaterEqualDepth = 5;
export const GreaterDepth = 6;
export const NotEqualDepth = 7;
export const MultiplyOperation = 0;
export const MixOperation = 1;
export const AddOperation = 2;
export const NoToneMapping = 0;
export const LinearToneMapping = 1;
export const ReinhardToneMapping = 2;
export const CineonToneMapping = 3;
export const ACESFilmicToneMapping = 4;
export const CustomToneMapping = 5;
export const AgXToneMapping = 6;
export const NeutralToneMapping = 7;
export const AttachedBindMode = 'attached';
export const DetachedBindMode = 'detached';

export const UVMapping = 300;
export const CubeReflectionMapping = 301;
export const CubeRefractionMapping = 302;
export const EquirectangularReflectionMapping = 303;
export const EquirectangularRefractionMapping = 304;
export const CubeUVReflectionMapping = 306;
export const RepeatWrapping = 1000;
export const ClampToEdgeWrapping = 1001;
export const MirroredRepeatWrapping = 1002;
export const NearestFilter = 1003;
export const NearestMipmapNearestFilter = 1004;
export const NearestMipMapNearestFilter = 1004;
export const NearestMipmapLinearFilter = 1005;
export const NearestMipMapLinearFilter = 1005;
export const LinearFilter = 1006;
export const LinearMipmapNearestFilter = 1007;
export const LinearMipMapNearestFilter = 1007;
export const LinearMipmapLinearFilter = 1008;
export const LinearMipMapLinearFilter = 1008;
export const UnsignedByteType = 1009;
export const ByteType = 1010;
export const ShortType = 1011;
export const UnsignedShortType = 1012;
export const IntType = 1013;
export const UnsignedIntType = 1014;
export const FloatType = 1015;
export const HalfFloatType = 1016;
export const UnsignedShort4444Type = 1017;
export const UnsignedShort5551Type = 1018;
export const UnsignedInt248Type = 1020;
export const UnsignedInt5999Type = 35902;
export const AlphaFormat = 1021;
export const RGBFormat = 1022;
export const RGBAFormat = 1023;
export const LuminanceFormat = 1024;
export const LuminanceAlphaFormat = 1025;
export const DepthFormat = 1026;
export const DepthStencilFormat = 1027;
export const RedFormat = 1028;
export const RedIntegerFormat = 1029;
export const RGFormat = 1030;
export const RGIntegerFormat = 1031;
export const RGBIntegerFormat = 1032;
export const RGBAIntegerFormat = 1033;

export const RGB_S3TC_DXT1_Format = 33776;
export const RGBA_S3TC_DXT1_Format = 33777;
export const RGBA_S3TC_DXT3_Format = 33778;
export const RGBA_S3TC_DXT5_Format = 33779;
export const RGB_PVRTC_4BPPV1_Format = 35840;
export const RGB_PVRTC_2BPPV1_Format = 35841;
export const RGBA_PVRTC_4BPPV1_Format = 35842;
export const RGBA_PVRTC_2BPPV1_Format = 35843;
export const RGB_ETC1_Format = 36196;
export const RGB_ETC2_Format = 37492;
export const RGBA_ETC2_EAC_Format = 37496;
export const RGBA_ASTC_4x4_Format = 37808;
export const RGBA_ASTC_5x4_Format = 37809;
export const RGBA_ASTC_5x5_Format = 37810;
export const RGBA_ASTC_6x5_Format = 37811;
export const RGBA_ASTC_6x6_Format = 37812;
export const RGBA_ASTC_8x5_Format = 37813;
export const RGBA_ASTC_8x6_Format = 37814;
export const RGBA_ASTC_8x8_Format = 37815;
export const RGBA_ASTC_10x5_Format = 37816;
export const RGBA_ASTC_10x6_Format = 37817;
export const RGBA_ASTC_10x8_Format = 37818;
export const RGBA_ASTC_10x10_Format = 37819;
export const RGBA_ASTC_12x10_Format = 37820;
export const RGBA_ASTC_12x12_Format = 37821;
export const RGBA_BPTC_Format = 36492;
export const RGB_BPTC_SIGNED_Format = 36494;
export const RGB_BPTC_UNSIGNED_Format = 36495;
export const RED_RGTC1_Format = 36283;
export const SIGNED_RED_RGTC1_Format = 36284;
export const RED_GREEN_RGTC2_Format = 36285;
export const SIGNED_RED_GREEN_RGTC2_Format = 36286;
export const LoopOnce = 2200;
export const LoopRepeat = 2201;
export const LoopPingPong = 2202;
export const InterpolateDiscrete = 2300;
export const InterpolateLinear = 2301;
export const InterpolateSmooth = 2302;
export const ZeroCurvatureEnding = 2400;
export const ZeroSlopeEnding = 2401;
export const WrapAroundEnding = 2402;
export const NormalAnimationBlendMode = 2500;
export const AdditiveAnimationBlendMode = 2501;
export const TrianglesDrawMode = 0;
export const TriangleStripDrawMode = 1;
export const TriangleFanDrawMode = 2;
export const BasicDepthPacking = 3200;
export const RGBADepthPacking = 3201;
export const RGBDepthPacking = 3202;
export const RGDepthPacking = 3203;
export const TangentSpaceNormalMap = 0;
export const ObjectSpaceNormalMap = 1;

// Color space string identifiers, matching CSS Color Module Level 4 and WebGPU names where available.
export const NoColorSpace = '';
export const SRGBColorSpace = 'srgb';
export const LinearSRGBColorSpace = 'srgb-linear';

export const LinearTransfer = 'linear';
export const SRGBTransfer = 'srgb';

export const ZeroStencilOp = 0;
export const KeepStencilOp = 7680;
export const ReplaceStencilOp = 7681;
export const IncrementStencilOp = 7682;
export const DecrementStencilOp = 7683;
export const IncrementWrapStencilOp = 34055;
export const DecrementWrapStencilOp = 34056;
export const InvertStencilOp = 5386;

export const NeverStencilFunc = 512;
export const LessStencilFunc = 513;
export const EqualStencilFunc = 514;
export const LessEqualStencilFunc = 515;
export const GreaterStencilFunc = 516;
export const NotEqualStencilFunc = 517;
export const GreaterEqualStencilFunc = 518;
export const AlwaysStencilFunc = 519;

export const NeverCompare = 512;
export const LessCompare = 513;
export const EqualCompare = 514;
export const LessEqualCompare = 515;
export const GreaterCompare = 516;
export const NotEqualCompare = 517;
export const GreaterEqualCompare = 518;
export const AlwaysCompare = 519;

export const StaticDrawUsage = 35044;
export const DynamicDrawUsage = 35048;
export const StreamDrawUsage = 35040;
export const StaticReadUsage = 35045;
export const DynamicReadUsage = 35049;
export const StreamReadUsage = 35041;
export const StaticCopyUsage = 35046;
export const DynamicCopyUsage = 35050;
export const StreamCopyUsage = 35042;

export const GLSL1 = '100';
export const GLSL3 = '300 es';

export const WebGLCoordinateSystem = 2000;
export const WebGPUCoordinateSystem = 2001;


