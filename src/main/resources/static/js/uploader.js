const {Uppy, Dashboard, Tus} = window.Uppy

const uppy = new Uppy()
    .use(Dashboard, {
        target: "#uploader",
        inline: true
    })
    .use(Tus, { //Tusプロトコルを指定
        endpoint: "/api/upload", //エンドポイントの指定
        headers: {[csrfHeader]: csrfToken}, //csrf対策用のトークンをヘッダーに入れる
    });