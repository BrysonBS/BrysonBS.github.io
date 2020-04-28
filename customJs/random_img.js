var hashCode = function (str) {
        if (!str && str.length === 0) {
            return 0;
        }

        var hash = 0;
        for (var i = 0, len = str.length; i < len; i++) {
            hash = ((hash << 5) - hash) + str.charCodeAt(i);
            hash |= 0;
        }
        return hash;
};
function sendAjax(page,index,$node){
    //ajax跨域请求
    $.ajax({
        url: "http://plugins.eagleget.com/wp.php",
        type: "GET",
        //sync: true,
        data: { "mt":"wps","page":page,"ned":"en-US"},
        dataType: "jsonp",
        success: function (data) {
            //console.log(data);
            var url = data.data.wps[index];
            //console.log(index);
            //console.log($node);
            if($node[0].tagName == "IMG"){
                $node.attr("src",url);
            }
            else{
                $node.css("background-image","url('"+url+"')");
            }
        }
    });
};
var getImgInfo = function(randomImg,$node){
    //console.log(randomImg);
    //console.log($node);
    if(!randomImg.enable) return;
    var xor_s = parseInt(new Date().getTime()/1000/randomImg.second);
    var pageImg = Math.abs((hashCode(randomImg.title) ^ xor_s) % randomImg.count);
    var indexImg = Math.abs((hashCode(randomImg.title) ^ xor_s) % randomImg.pageCount);
    sendAjax(pageImg,indexImg,$node);
};
