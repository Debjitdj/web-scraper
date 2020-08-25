/**
 * no any depend selector calculate helper method
 * used for extension and tests
 */

/**
 * get tag
 * @param element the element
 * @returns {string} the tag
 */
function getTag(element) {
  const e = (element || '').trim();
  if (e.indexOf(':') > 0) {
    return e.split(':').shift().trim();
  }
  if (e.indexOf('.') > 0) {
    return e.split('.').shift().trim();
  }
  return e;
}

/**
 * Gets :nth-of-type(n)
 * @param {string} element Selector element.
 * @return {string} :nth-of-type(n) segment of element, or undefined, if
 *  the element is undefined, or does not have the segment.
 */
function getNthOfType(element) {
  let res;
  if (element) {
    const m = element.match(/:nth-of-type\(\d*\)/);
    if (m) [res] = m;
  }
  return res;
}

export const GET_COMMON_PARENT_MODES = {
  FROM_BEG: 'FROM_BEG',
  FROM_END: 'FROM_END',
};

/**
 * Given two selector fragments, it checks whether they have :nth-of-type(n)
 * components. If both do have them, or both does not have them, it returns
 * both fragments as is. If only one of the fragments has :nth-of-type(n)
 * component, then it returns both fragments with :nth-of-type(n) stripped
 * from the fragment having it.
 * @param {string} str1
 * @param {string} str2
 * @return {string[]} Array of two elements, generated as described above.
 */
function withoutAdditionalArray(str1, str2) {
  const res = [str1, str2];
  const nth1 = getNthOfType(str1);
  const nth2 = getNthOfType(str2);

  // True if, and only if, one of fragments has :nth-of-type(n) component,
  // and the other does not have it.
  if (!nth1 !== !nth2) {
    if (nth1) res[0] = str1.replace(nth1, '');
    else res[1] = str2.replace(nth2, '');
  }

  return res;
}

/**
 * get common parent
 * @param p1 path 1
 * @param p2 path 2
 * @param getI18T get i18t
 * @param {GET_COMMON_PARENT_MODES} [mode=GET_COMMON_PARENT_MODES.FROM_BEG]
 */
export function getCommonParent(
  p1,
  p2,
  getI18T,
  mode = GET_COMMON_PARENT_MODES.FROM_BEG,
) {
  const parts1 = p1.split('>');
  const parts2 = p2.split('>');
  const commonParts = [];

  if (parts1.length !== parts2.length) {
    throw new Error(
      `${getI18T()('editor.differentType')}\n\nSelector 1:\n${p1}\n\nSelector 2:\n${p2}`,
    );
  }
  const { length } = parts1;

  for (let i = 0; i < length; i++) {
    const tag1 = getTag(parts1[i]);
    const tag2 = getTag(parts2[i]);
    if (tag1 !== tag2) {
      throw new Error(
        `${getI18T()('editor.differentType')}\n${p1}\n${p2}`,
      );
    }
  }
  switch (mode) {
    case GET_COMMON_PARENT_MODES.FROM_BEG: {
      for (let i = 0; i < length; i++) {
        const [pp1, pp2] = withoutAdditionalArray(parts1[i], parts2[i]);
        if (pp1 === pp2) {
          commonParts.push(pp1);
        } else {
          commonParts.push(getTag(pp1));
          break;
        }
      }
      break;
    }
    case GET_COMMON_PARENT_MODES.FROM_END: {
      for (let i = length - 1; i >= 0; --i) {
        let [pp1, pp2] = withoutAdditionalArray(parts1[i], parts2[i]);
        if (pp1 !== pp2) {
          for (let j = 0; j <= i; ++j) {
            [pp1, pp2] = withoutAdditionalArray(parts1[j], parts2[j]);
            if (pp1 === pp2) commonParts.push(pp1);
            else commonParts.push(getTag(pp2));
          }
          break;
        }
      }
      break;
    }
    default: throw Error(`Invalid mode: ${mode}`);
  }
  return commonParts.map((p) => p.trim()).join(' > ');
}

/**
 * get p1 parent (common part + p1 part, common part + p2 part)
 * @param p1 the path 1
 * @param p2 the path 2
 */
export function getPathParent(p1, p2) {
  const parts1 = p1.split('>');
  const parts2 = p2.split('>');
  const minLength = Math.min(parts1.length, parts2.length);
  const commonParts = [];
  const results = [];
  for (let i = 0; i < minLength; i++) {
    if (parts1[i] === parts2[i]) {
      commonParts.push(parts1[i]);
    } else {
      results[0] = commonParts.concat([parts1[i]]).map((p) => p.trim()).join(' > ');
      results[1] = commonParts.concat([parts2[i]]).map((p) => p.trim()).join(' > ');
      break;
    }
  }
  return results;
}

/**
 * remove parent
 * @param parent the parent path
 * @param path the current path
 */
export function removeParent(parent, path) {
  const parentParts = parent.split('>');
  const parts = path.split('>');
  for (let i = 0; i < parentParts.length; i++) {
    const tag1 = getTag(parts[0]);
    const tag2 = getTag(parentParts[i]);
    if (tag1 === tag2) {
      parts.shift();
    }
  }
  return parts.map((p) => p.trim()).join(' > ');
}

/**
 * get common class
 * @param classes the classes
 * @return {string}
 */
export function getCommonClass(classes) {
  if (!classes || classes.length <= 0) {
    return '';
  }
  let common = (classes[0] || '').split('.');
  for (let i = 1; i < classes.length; i++) {
    const parts = (classes[i] || '').split('.');
    const newCommon = [];
    for (let ii = 0; ii < common.length; ii++) {
      if (common[ii] === parts[ii]) {
        newCommon.push(common[ii]);
      }
    }
    common = newCommon;
  }
  return common.join('.');
}

/**
 * Remove different and additional ‘:nth-of-type()’ array number in pair of selectors
 * @param p1 the path 1
 * @param p2 the path 2
 * @return {string}
 */
export function removeDifferentAndAdditional(p1, p2) {
  const parts1 = p1.split('>');
  const parts2 = p2.split('>');
  // part2 length should = part2 length
  for (let i = 0; i < parts1.length; i++) {
    const tag1 = getTag(parts1[i]);
    const tag2 = getTag(parts2[i]);
    if (tag1 === tag2 && parts1[i] !== parts2[i]) {
      // Remove different and additional `:nth-of-type()` array number in pair of selectors
      parts1[i] = tag1;
      parts2[i] = tag2;
    }
  }
  return parts2.map((p) => p.trim()).join(' > ');
}

/**
 * Joins selectors by " > " separator, ignoring empty selectors.
 * @param  {...string} args Any number of selector strings.
 * @return {string}
 */
export function joinSelectors(...args) {
  let res = '';
  args.forEach((selector) => {
    if (selector) {
      if (res) res += ' > ';
      res += selector;
    }
  });
  return res;
}
