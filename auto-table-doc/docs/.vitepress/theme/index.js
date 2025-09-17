import DefaultTheme from 'vitepress/theme'

export default {
    ...DefaultTheme,
    enhanceApp({app, router, siteData}) {
        // 百度统计代码
        const script = document.createElement('script')
        script.innerHTML = `
          var _hmt = _hmt || [];
          (function() {
            var hm = document.createElement("script");
            hm.src = "https://hm.baidu.com/hm.js?bfab71ad68558ed5bb57ade879a6da84";
            var s = document.getElementsByTagName("script")[0]; 
            s.parentNode.insertBefore(hm, s);
          })();
        `
        document.head.appendChild(script)
    }
}
