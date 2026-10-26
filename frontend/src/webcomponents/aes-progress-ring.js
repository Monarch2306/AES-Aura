class AESProgressRing extends HTMLElement {
  static get observedAttributes(){ return ['value','label'] }
  connectedCallback(){ this.render() }
  attributeChangedCallback(){ this.render() }
  render(){
    const value=Math.max(0,Math.min(100,Number(this.getAttribute('value')||0)));
    const label=this.getAttribute('label')||`${Math.round(value)}%`;
    this.innerHTML=`<div class="aes-ring" style="--p:${value}"><div><strong>${label}</strong><small>confidence</small></div></div>`
  }
}
if(!customElements.get('aes-progress-ring')) customElements.define('aes-progress-ring',AESProgressRing)
