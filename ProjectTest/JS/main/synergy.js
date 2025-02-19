// synergy/attribute.js

import { isPrimitive, typeOf } from "./helpers.js"

const pascalToKebab = (string) =>
  string.replace(/[\w]([A-Z])/g, function (m) {
    return m[0] + "-" + m[1].toLowerCase()
  })

const kebabToPascal = (string) =>
  string.replace(/[\w]-([\w])/g, function (m) {
    return m[0] + m[2].toUpperCase()
  })

const parseStyles = (value) => {
  let type = typeof value

  if (type === "string")
    return value.split(";").reduce((o, value) => {
      const [k, v] = value.split(":").map((v) => v.trim())
      if (k) o[k] = v
      return o
    }, {})

  if (type === "object") return value

  return {}
}

const joinStyles = (value) =>
  Object.entries(value)
    .map(([k, v]) => `${k}: ${v};`)
    .join(" ")

const convertStyles = (o) =>
  Object.keys(o).reduce((a, k) => {
    a[pascalToKebab(k)] = o[k]
    return a
  }, {})

export const applyAttribute = (node, name, value) => {
  if (name === "style") {
    value = joinStyles(
      convertStyles({
        ...parseStyles(node.getAttribute("style")),
        ...parseStyles(value),
      })
    )
  } else if (name === "class") {
    switch (typeOf(value)) {
      case "Array":
        value = value.join(" ")
        break
      case "Object":
        value = Object.keys(value)
          .reduce((a, k) => {
            if (value[k]) a.push(k)
            return a
          }, [])
          .join(" ")
        break
    }
  } else if (!isPrimitive(value)) {
    return (node[kebabToPascal(name)] = value)
  }

  name = pascalToKebab(name)

  if (typeof value === "boolean") {
    if (name.startsWith("aria-")) {
      value = "" + value
    } else if (value) {
      value = ""
    }
  }

  let current = node.getAttribute(name)

  if (value === current) return

  if (typeof value === "string" || typeof value === "number") {
    node.setAttribute(name, value)
  } else {
    node.removeAttribute(name)
  }
}


// synergy/css.js

function nextWord(css, count) {
  return css.slice(count - 1).split(/[\s+|\n+|,]/)[0]
}

function nextOpenBrace(css, count) {
  let index = css.slice(count - 1).indexOf("{")
  if (index > -1) {
    return count + index
  }
}

export function prefixSelectors(prefix, css) {
  let insideBlock = false
  let look = true
  let output = ""
  let count = 0
  let skip = false

  for (let char of css) {
    if (char === "@" && nextWord(css, count + 1) === "@media") {
      skip = nextOpenBrace(css, count)
    }

    if (skip) {
      if (skip === count) skip = false
    }

    if (!skip) {
      if (char === "}") {
        insideBlock = false
        look = true
      } else if (char === ",") {
        look = true
      } else if (char === "{") {
        insideBlock = true
      } else if (look && !insideBlock && !char.match(/\s/)) {
        let w = nextWord(css, count + 1)

        // console.log({ w })

        if (
          w !== prefix &&
          w.charAt(0) !== "@" &&
          w.charAt(0) !== ":" &&
          w.charAt(0) !== "*" &&
          w !== "html" &&
          w !== "body"
        ) {
          output += prefix + " "
        }
        look = false
      }
    }
    output += char
    count += 1
  }

  return output
}

export function appendStyles(name, css) {
  if (document.querySelector(`style#${name}`)) return

  const el = document.createElement("style")
  el.id = name
  el.innerHTML = prefixSelectors(name, css)
  document.head.appendChild(el)
}


// synergy/helpers.js

export const wrapToken = (v) => {
  v = v.trim()
  if (v.startsWith("{{")) return v
  return `{{${v}}}`
}

export const last = (v = []) => v[v.length - 1]

export const isWhitespace = (node) => {
  return node.nodeType === node.TEXT_NODE && node.nodeValue.trim() === ""
}

export const walk = (node, callback, deep = true) => {
  if (!node) return
  // if (node.matches?.(`script[type="application/synergy"]`))
  //   return walk(node.nextSibling, callback, deep)
  if (!isWhitespace(node)) {
    let v = callback(node)
    if (v === false) return
    if (v?.nodeName) return walk(v, callback, deep)
  }
  if (deep) walk(node.firstChild, callback, deep)
  walk(node.nextSibling, callback, deep)
}

const transformBrackets = (str = "") => {
  let parts = str.split(/(\[[^\]]+\])/).filter((v) => v)
  return parts.reduce((a, part) => {
    let v = part.charAt(0) === "[" ? "." + part.replace(/\./g, ":") : part
    return a + v
  }, "")
}

const getTarget = (path, target) => {
  let parts = transformBrackets(path)
    .split(".")
    .map((k) => {
      if (k.charAt(0) === "[") {
        let p = k.slice(1, -1).replace(/:/g, ".")
        return getValueAtPath(p, target)
      } else {
        return k
      }
    })

  let t =
    parts.slice(0, -1).reduce((o, k) => {
      return o && o[k]
    }, target) || target
  return [t, last(parts)]
}

export const getValueAtPath = (path, target) => {
  let [a, b] = getTarget(path, target)
  let v = a?.[b]
  if (typeof v === "function") return v.bind(a)
  return v
}

export const setValueAtPath = (path, value, target) => {
  let [a, b] = getTarget(path, target)
  return (a[b] = value)
}

export const fragmentFromTemplate = (v) => {
  if (typeof v === "string") {
    if (v.charAt(0) === "#") {
      v = document.querySelector(v)
    } else {
      let tpl = document.createElement("template")
      tpl.innerHTML = v.trim()
      return tpl.content.cloneNode(true)
    }
  }
  if (v.nodeName === "TEMPLATE") return v.cloneNode(true).content
  if (v.nodeName === "defs") return v.firstElementChild.cloneNode(true)
}

export const debounce = (fn) => {
  let wait = false
  let invoke = false
  return () => {
    if (wait) {
      invoke = true
    } else {
      wait = true
      fn()
      requestAnimationFrame(() => {
        if (invoke) fn()
        wait = false
      })
    }
  }
}

export const isPrimitive = (v) => v === null || typeof v !== "object"

export const typeOf = (v) =>
  Object.prototype.toString.call(v).match(/\s(.+[^\]])/)[1]

export const pascalToKebab = (string) =>
  string.replace(/[\w]([A-Z])/g, function (m) {
    return m[0] + "-" + m[1].toLowerCase()
  })

export const kebabToPascal = (string) =>
  string.replace(/[\w]-([\w])/g, function (m) {
    return m[0] + m[2].toUpperCase()
  })

export const applyAttribute = (node, name, value) => {
  name = pascalToKebab(name)

  if (typeof value === "boolean") {
    if (name.startsWith("aria-")) {
      value = "" + value
    } else if (value) {
      value = ""
    }
  }

  if (typeof value === "string" || typeof value === "number") {
    node.setAttribute(name, value)
  } else {
    node.removeAttribute(name)
  }
}

export const attributeToProp = (k, v) => {
  let name = kebabToPascal(k)
  if (v === "") v = true
  if (k.startsWith("aria-")) {
    if (v === "true") v = true
    if (v === "false") v = false
  }
  return {
    name,
    value: v,
  }
}

export function getDataScript(node) {
  return node.querySelector(
    `script[type="application/synergy"][id="${node.nodeName}"]`
  )
}

export function createDataScript(node) {
  let ds = document.createElement("script")
  ds.setAttribute("type", "application/synergy")
  ds.setAttribute("id", node.nodeName)
  node.append(ds)
  return ds
}


// synergy/partial.js

import { appendStyles } from "./css.js"

export const partials = {}

export const partial = (name, html, css) => {
  if (css) appendStyles(name, css)
  partials[name.toUpperCase()] = html
}


// synergy/context.js

import { getValueAtPath, isPrimitive } from "./helpers.js"

const handler = ({ path, identifier, key, index, i, k }) => ({
  get(target, property) {
    let x = getValueAtPath(path, target)

    // x === the collection

    if (property === identifier) {
      for (let n in x) {
        let v = x[n]
        if (key) {
          if (v[key] === k) return v
        } else {
          if (n == i) return v
        }
      }
    }

    if (property === index) {
      for (let n in x) {
        let v = x[n]
        if (key) {
          if (v[key] === k) return n
        } else {
          if (n == i) return n
        }
      }
    }

    let t = key ? x.find((v) => v[key] === k) : x?.[i]
    if (t?.hasOwnProperty?.(property)) return t[property]

    return Reflect.get(...arguments)
  },
  set(target, property, value) {
    let x = getValueAtPath(path, target)
    let t = key ? x.find((v) => v[key] === k) : x[i]
    if (t && !isPrimitive(t)) {
      t[property] = value
      return true
    }

    return Reflect.set(...arguments)
  },
})

export const createContext = (v = []) => {
  let context = v
  return {
    get: () => context,
    push: (v) => context.push(v),
    wrap: (state) => {
      return context.reduce(
        (target, ctx) => new Proxy(target, handler(ctx)),
        state
      )
    },
  }
}


