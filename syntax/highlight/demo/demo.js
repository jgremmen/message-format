// Shared demo code: renders the sample messages, a theme switcher and toggles for the CSS files.
// Call renderDemo(hljs) after the message-format language has been registered.

(function() {
  const CDN = 'https://cdn.jsdelivr.net/npm/@highlightjs/cdn-assets@11.12.0/styles/';
  const THEMES = [
    'default', 'github', 'github-dark', 'atom-one-light', 'atom-one-dark', 'monokai', 'nord',
    'stackoverflow-light', 'tokyo-night-dark', 'vs2015'
  ];

  const SAMPLES = [
    'Simple text with escapes: \\u0041 \\x41 \\\' \\" \\% \\{ \\\\ \\[',
    'Enum key type %{p,empty:empty,!empty:\'not empty\',=null:null,!null:\'not null\',true:true,false:false}',
    'Name key type %{p,richtig:true,falsch:false,size:-678764782,text:\'text\',message:\'msg %{m}\'}.',
    'Number key type %{p, <10:A, <=20:B, =30:C, >1999:D, >=200:E, !25:F, :G}',
    'String key type %{p, format:mychar, <\'A\':A, >\'Z\':Z, :P}',
    'Something went wrong while parsing %[result,n->count,p=true]%[ex-colon]',
    'Additional info: %(clip,\'%{s,empty:x}\',clip:34)',
    '%{n,1:\'1 result\',:\'%{n} results\'}',
    '%{count,format:choice,0:\'no items\',1:\'one item\',:\'%{count} items\'} in %[folder,name->folder-name,\n  shared=true] %(clip,\'%{description,format:string}\',size:40,ellipsis:\'…\')',
    'Grouped keys %{p,(1,2,3):\'small\',<>\'x\':\'not x\',:\'default\'}',
    'Invalid input %{p,@} and unfinished %[t,x='
  ];

  window.renderDemo = function(hljs) {
    const themeLink = document.getElementById('theme');
    const baseLink = document.getElementById('base');
    const baseToggle = document.getElementById('base-toggle');
    const fallbackLink = document.getElementById('fallback');
    const select = document.getElementById('theme-select');
    const toggle = document.getElementById('fallback-toggle');
    const samples = document.getElementById('samples');

    for(const theme of THEMES)
      select.add(new Option(theme, theme));

    select.addEventListener('change', () => { themeLink.href = `${CDN}${select.value}.min.css`; });
    toggle.addEventListener('change', () => { fallbackLink.disabled = !toggle.checked; });
    fallbackLink.disabled = !toggle.checked;
    baseToggle.addEventListener('change', () => { baseLink.disabled = !baseToggle.checked; });
    baseLink.disabled = !baseToggle.checked;

    for(const sample of SAMPLES)
    {
      const pre = document.createElement('pre');
      const code = document.createElement('code');

      code.className = 'language-message-format';
      code.textContent = sample;
      pre.appendChild(code);
      samples.appendChild(pre);

      hljs.highlightElement(code);
    }
  };
})();
