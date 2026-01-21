import React, { useRef } from 'react';

export const Port = (props) => {
    const { port, x, z, showPorts, portVisible, getPoint, AnchorIcon, showPortTooltip, removePortTooltip } = props;
    
    const center = getPoint(x, z);
    
    // Get icon size based on map scale
    const iconSizeFromMapScale = getPoint(16, 0).x - getPoint(0, 0).x;
    let iconSize = 20;
    if (iconSizeFromMapScale === 4) {
        iconSize = 20;
    } else if (iconSizeFromMapScale === 8) {
        iconSize = 20;
    } else if (iconSizeFromMapScale === 16) {
        iconSize = 32;
    } else if (iconSizeFromMapScale === 32) {
        iconSize = 48;
    } else if (iconSizeFromMapScale > 32) {
        iconSize = 64;
    }

    const imageRef = useRef(null);

    // Pass the mouse event to get accurate screen coordinates
    const handleMouseEnter = (event) => {
        showPortTooltip(port, event.clientX, event.clientY);
    };

    const handleMouseMove = (event) => {
        showPortTooltip(port, event.clientX, event.clientY);
    };

    if (!showPorts || !portVisible) {
        return null;
    }

    const cx = center.x - iconSize / 2;
    const cy = center.y - iconSize / 2;

    return (
        <g>
            <image 
                ref={imageRef}
                x={cx} 
                y={cy} 
                width={iconSize} 
                height={iconSize} 
                href={AnchorIcon}
                onMouseEnter={handleMouseEnter}
                onMouseMove={handleMouseMove}
                onMouseLeave={removePortTooltip}
                style={{ cursor: 'pointer' }}
            />
        </g>
    );
};

export const PortTooltip = (props) => {
    const { clientX, clientY, port, enable } = props;

    if (!enable || !port) {
        return null;
    }

    // Position to the top-right of the cursor, similar to the image
    return (
        <div 
            id="port-tooltip" 
            style={{ 
                position: 'fixed',
                left: `${clientX + 20}px`,   // 20px to the right of cursor
                top: `${clientY - 80}px`,    // Position above cursor
                pointerEvents: 'none'
            }}
        >
            <div><b>Port:</b> {port.name}</div>
            <div><b>Groups:</b> {port.groupsString}</div>
            <div><b>x:</b> {Math.round(port.x)}</div>
            <div><b>z:</b> {Math.round(port.z)}</div>
        </div>
    );
};