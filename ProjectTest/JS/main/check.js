// check/check.js

import { isSubclass } from './subclass.js'

// Confirm `custom` option is valid
export const checkCustom = (custom, ParentError) => {
  if (typeof custom !== 'function') {
    throw new TypeError(
      `The "custom" class of "${ParentError.name}.subclass()" must be a class: ${custom}`,
    )
  }

  checkParent(custom, ParentError)
  checkPrototype(custom, ParentError)
}

// We do not allow passing `ParentError` without extending from it, since
// `undefined` can be used for it instead.
// We do not allow extending from `ParentError` indirectly:
//  - This promotes using subclassing through `ErrorClass.subclass()`, since it
//    reduces the risk of user instantiating unregistered class
//  - This promotes `ErrorClass.subclass()` as a pattern for subclassing, to
//    reduce the risk of directly extending a registered class without
//    registering the subclass
const checkParent = (custom, ParentError) => {
  if (custom === ParentError) {
    throw new TypeError(
      `The "custom" class of "${ParentError.name}.subclass()" must extend from ${ParentError.name}, but not be ${ParentError.name} itself.`,
    )
  }

  if (!isSubclass(custom, ParentError)) {
    throw new TypeError(
      `The "custom" class of "${ParentError.name}.subclass()" must extend from ${ParentError.name}.`,
    )
  }

  if (Object.getPrototypeOf(custom) !== ParentError) {
    throw new TypeError(
      `The "custom" class of "${ParentError.name}.subclass()" must extend directly from ${ParentError.name}.`,
    )
  }
}

const checkPrototype = (custom, ParentError) => {
  if (typeof custom.prototype !== 'object' || custom.prototype === null) {
    throw new TypeError(
      `The "custom" class's prototype of "${ParentError.name}.subclass()" is invalid: ${custom.prototype}`,
    )
  }

  if (custom.prototype.constructor !== custom) {
    throw new TypeError(
      `The "custom" class of "${ParentError.name}.subclass()" has an invalid "constructor" property.`,
    )
  }
}


// check/subclass.js

// Check if `ErrorClass` is a subclass of `ParentClass`.
// We encourage `instanceof` over `error.name` for checking since this:
//  - Prevents name collisions with other libraries
//  - Allows checking if any error came from a given library
//  - Includes error classes in the exported interface explicitly instead of
//    implicitly, so that users are mindful about breaking changes
//  - Bundles classes with TypeScript documentation, types and autocompletion
//  - Encourages documenting error types
// Checking class with `error.name` is still supported, but not documented
//  - Since it is widely used and can be better in specific cases
// This also provides with namespacing, i.e. prevents classes of the same name
// but in different libraries to be considered equal. As opposed to the
// following alternatives:
//  - Namespacing all error names with a common prefix since this:
//     - Leads to verbose error names
//     - Requires either an additional option, or guessing ambiguously whether
//       error names are meant to include a namespace prefix
//  - Using a separate `namespace` property: this adds too much complexity and
//    is less standard than `instanceof`
export const isSubclass = (ErrorClass, ParentClass) =>
  ParentClass === ErrorClass || isProtoOf.call(ParentClass, ErrorClass)

const { isPrototypeOf: isProtoOf } = Object.prototype


