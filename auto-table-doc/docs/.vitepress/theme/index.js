import DefaultTheme from 'vitepress/theme'

export default {
    ...DefaultTheme,
    enhanceApp({app, router, siteData}) {
        // 确保只在客户端执行
        if (typeof window !== 'undefined' && typeof document !== 'undefined') {
            const script = document.createElement('script')
            script.innerHTML = `
                var _hmt = _hmt || [];
                (function() {
                  var hm = document.createElement("script");
                  hm.src = "https://hm.baidu.com/hm.js?bfab71ad68558ed5bb57ade879a6da84";
                  var s = document.getElementsByTagName("script")[0]; 
                  s.parentNode.insertBefore(hm, s);
                })();
                
                (function(c,l,a,r,i,t,y){
                    c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};
                    t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;
                    y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);
                })(window, document, "clarity", "script", "tcfzccu52g");
              `
            document.head.appendChild(script)
        }
    }
}
