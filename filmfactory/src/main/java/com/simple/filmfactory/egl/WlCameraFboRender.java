package com.simple.filmfactory.egl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.simple.filmfactory.R;
import com.simple.filmfactory.egl.base.WlShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 单独处理fbo的纹理
 * 用于添加水印等特效，提高了渲染效率
 **/
public class WlCameraFboRender {

    private Context context;

    /**
     * 8个顶点着色器坐标，除了绘制界面，还需要绘制水印
     **/
    private float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,

            0f, 0f,
            0f, 0f,
            0f, 0f,
            0f, 0f
    };
    private FloatBuffer vertexBuffer;

    /**
     * 四个片元着色器坐标
     **/
    private float[] fragmentData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };
    private FloatBuffer fragmentBuffer;

    private int program;
    private int vPosition;
    private int fPosition;
    private int sampler;

    private int vboId;

    private Bitmap bitmap;
    private int bitmapTextureid;

    public WlCameraFboRender(Context context) {
        this.context = context;

        bitmap = WlShaderUtil.createTextImage(context, "内涵段子tv", 50, "#ff0000", "#00000000", 0);


        float r = 1.0f * bitmap.getWidth() / bitmap.getHeight();
        float w = r * 0.1f;

        vertexData[8] = 0.8f - w;
        vertexData[9] = -0.8f;

        vertexData[10] = 0.8f;
        vertexData[11] = -0.8f;

        vertexData[12] = 0.8f - w;
        vertexData[13] = -0.7f;

        vertexData[14] = 0.8f;
        vertexData[15] = -0.7f;

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        fragmentBuffer = ByteBuffer.allocateDirect(fragmentData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(fragmentData);
        fragmentBuffer.position(0);

    }

    public void onCreate() {

        //开启透明，不然水印会有背景
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        String vertexSource = WlShaderUtil.getRawResource(context, R.raw.vertex_shader_screen);
        String fragmentSource = WlShaderUtil.getRawResource(context, R.raw.fragment_shader_screen);

        program = WlShaderUtil.createProgram(vertexSource, fragmentSource);

        vPosition = GLES20.glGetAttribLocation(program, "v_Position");
        fPosition = GLES20.glGetAttribLocation(program, "f_Position");
        sampler = GLES20.glGetUniformLocation(program, "sTexture");

        //创建vbo
        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vboId = vbos[0];

        //绑定vbo并初始化控件
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + fragmentData.length * 4, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, fragmentData.length * 4, fragmentBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        bitmapTextureid = WlShaderUtil.loadBitmapTexture(bitmap);
    }

    private int width;
    private int height;

    public void onChange(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void onDraw(int textureId) {
        ////把窗口清除为当前颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //设置清除颜色
        GLES20.glClearColor(1f, 0f, 0f, 1f);

        GLES20.glViewport(0, 0, width, height);

        GLES20.glUseProgram(program);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        //通过vPosition激活对应的属性，才可以正常的读写数据
        GLES20.glEnableVertexAttribArray(vPosition);
        //用于设置顶点数据解析方式
        //第一个参数指定从索引开始取数据，与顶点着色器中layout(location=0)对应。
        //第二个参数指定顶点属性大小。
        //第三个参数指定数据类型。
        //第四个参数定义是否希望数据被标准化（归一化），只表示方向不表示大小。
        //第五个参数是步长（Stride），指定在连续的顶点属性之间的间隔。
        //第六个参数表示我们的位置数据在缓冲区起始位置的偏移量。
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 2 * 4,
                0);
        GLES20.glEnableVertexAttribArray(fPosition);
        //激活属性并进行赋值，需要注意的是，如果使用vbo，最后一个参数传入偏移量即可，如果没用vbo，就传入顶点和纹理缓存
        //这里是因为使用来vbo，缓存了顶点和片元的全部坐标，所以使用给片元坐标操作时，需要偏移整个顶点坐标的数据量
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 2 * 4,
                vertexData.length * 4);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


        //绘制bitmap水印
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTextureid);
        GLES20.glEnableVertexAttribArray(vPosition);
        //这里的32指的是使用后面的4个顶点坐标来绘制水印
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 2 * 4,
                32);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 2 * 4,
                vertexData.length * 4);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}