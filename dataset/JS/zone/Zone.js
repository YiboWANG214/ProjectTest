import Vector2D from "./Vector2D";

export default class Zone {
  constructor() {
    this.vector = new Vector2D(0, 0);
    this.random = 0;
    this.crossType = "dead";
    this.alert = true;
  }

  getPosition() {}

  crossing(particle) {}

  destroy() {
    this.vector = null;
  }
}
