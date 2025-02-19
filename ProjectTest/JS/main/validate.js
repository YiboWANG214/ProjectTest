// validate/map.js

// We use a global `WeakMap` to store class-specific information (such as
// options) instead of storing it as a symbol property on each error class to
// ensure:
//  - This is not exposed to users or plugin authors
//  - This does not change how the error class is printed
// We use a `WeakMap` instead of an object since the key should be the error
// class, not its `name`, because classes might have duplicate names.
export const classesData = new WeakMap()

// The same but for error instances
export const instancesData = new WeakMap()


// validate/validate.js

import { classesData } from './map.js'

// We forbid subclasses that are not known, i.e. not passed to
// `ErrorClass.subclass()`
//  - They would not be validated at load time
//  - The class would not be normalized until its first instantiation
//     - E.g. its `prototype.name` might be missing
//  - The list of `ErrorClasses` would be potentially incomplete
//     - E.g. `ErrorClass.parse()` would not be able to parse an error class
//       until its first instantiation
// This usually happens if a class was:
//  - Not passed to the `custom` option of `*Error.subclass()`
//  - But was extended from a known class
export const validateSubclass = (ErrorClass) => {
  if (classesData.has(ErrorClass)) {
    return
  }

  const { name } = ErrorClass
  const { name: parentName } = Object.getPrototypeOf(ErrorClass)
  throw new Error(
    `"new ${name}()" must not be directly called.
This error class should be created like this instead:
  export const ${name} = ${parentName}.subclass('${name}')`,
  )
}


