<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:param name="orderId" select="'N/A'"/>
    <xsl:param name="orderDate" select="'N/A'"/>
    <xsl:param name="customerName" select="'N/A'"/>
    <xsl:param name="customerPhone" select="'N/A'"/>
    <xsl:param name="totalAmount" select="'0.00'"/>
    <xsl:param name="currencySymbol" select="'Rs.'"/>
    <xsl:param name="itemsXml" select="'&lt;items/&gt;'"/>

    <xsl:template match="/">
        <fo:root>
            <fo:layout-master-set>
                <fo:simple-page-master master-name="A4"
                                       page-height="29.7cm" page-width="21cm"
                                       margin-top="1.5cm" margin-bottom="1.5cm"
                                       margin-left="1.5cm" margin-right="1.5cm">
                    <fo:region-body/>
                    <fo:region-after extent="1.5cm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="A4">

                <fo:static-content flow-name="xsl-region-after">
                    <fo:block text-align="center" font-size="9pt" font-style="italic" color="#6B7280">
                        Thank you for your business! | www.increff.com
                    </fo:block>
                </fo:static-content>

                <fo:flow flow-name="xsl-region-body">

                    <fo:block-container background-color="#1F2937" color="#FFFFFF"
                                        margin-left="-1.5cm" margin-right="-1.5cm"
                                        padding-left="1.5cm" padding-right="1.5cm">
                        <fo:table table-layout="fixed" width="100%">
                            <fo:table-column column-width="50%"/>
                            <fo:table-column column-width="50%"/>
                            <fo:table-body>
                                <fo:table-row height="65pt">
                                    <fo:table-cell>
                                        <fo:block font-size="20pt" font-weight="bold"
                                                  padding-before="22.5pt">Increff POS</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell>
                                        <fo:block font-size="36pt" font-weight="bold" text-align="right"
                                                  padding-before="14.5pt">INVOICE</fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block-container>

                    <fo:table table-layout="fixed" width="100%" margin-top="1cm" margin-bottom="1cm">
                        <fo:table-column column-width="50%"/>
                        <fo:table-column column-width="50%"/>
                        <fo:table-body>
                            <fo:table-row>
                                <fo:table-cell vertical-align="top">
                                    <fo:block font-weight="bold" font-size="10pt" color="#6B7280">BILL TO</fo:block>
                                    <fo:block font-weight="bold" font-size="12pt" color="#111827" margin-top="2pt">
                                        <xsl:value-of select="$customerName"/>
                                    </fo:block>
                                    <fo:block font-size="10pt" color="#6B7280" margin-top="2pt">
                                        Phone: <xsl:value-of select="$customerPhone"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell vertical-align="top" text-align="right">
                                    <fo:block font-size="9pt" color="#6B7280">
                                        Order ID: #<xsl:value-of select="$orderId"/>
                                    </fo:block>
                                    <fo:block font-size="9pt" color="#6B7280" margin-top="2pt">
                                        Date: <xsl:value-of select="$orderDate"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-body>
                    </fo:table>

                    <fo:table table-layout="fixed" width="100%" border-collapse="collapse">
                        <fo:table-column column-width="10%"/>
                        <fo:table-column column-width="35%"/>
                        <fo:table-column column-width="15%"/>
                        <fo:table-column column-width="20%"/>
                        <fo:table-column column-width="20%"/>

                        <fo:table-header>
                            <fo:table-row background-color="#F3F4F6" font-weight="bold" font-size="10pt" color="#6B7280">
                                <fo:table-cell padding="8pt 5pt"><fo:block>S.No</fo:block></fo:table-cell>
                                <fo:table-cell padding="8pt 5pt"><fo:block>Item Description</fo:block></fo:table-cell>
                                <fo:table-cell padding="8pt 5pt"><fo:block text-align="right">Qty</fo:block></fo:table-cell>
                                <fo:table-cell padding="8pt 5pt"><fo:block text-align="right">Unit Price</fo:block></fo:table-cell>
                                <fo:table-cell padding="8pt 5pt"><fo:block text-align="right">Total</fo:block></fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>

                        <fo:table-body>
                            <xsl:variable name="itemsDoc" select="parse-xml($itemsXml)"/>
                            <xsl:for-each select="$itemsDoc/items/item">
                                <fo:table-row border-bottom="1pt solid #E5E7EB">
                                    <xsl:if test="position() mod 2 = 0">
                                        <xsl:attribute name="background-color">#F9FAFB</xsl:attribute>
                                    </xsl:if>

                                    <fo:table-cell padding="8pt 5pt" vertical-align="top"><fo:block><xsl:value-of select="position()"/></fo:block></fo:table-cell>
                                    <fo:table-cell padding="8pt 5pt" vertical-align="top"><fo:block><xsl:value-of select="productName"/></fo:block></fo:table-cell>
                                    <fo:table-cell padding="8pt 5pt" vertical-align="top"><fo:block text-align="right"><xsl:value-of select="quantity"/></fo:block></fo:table-cell>
                                    <fo:table-cell padding="8pt 5pt" vertical-align="top"><fo:block text-align="right"><xsl:value-of select="$currencySymbol"/> <xsl:value-of select="sellingPrice"/></fo:block></fo:table-cell>
                                    <fo:table-cell padding="8pt 5pt" vertical-align="top"><fo:block text-align="right" font-weight="bold"><xsl:value-of select="$currencySymbol"/> <xsl:value-of select="total"/></fo:block></fo:table-cell>
                                </fo:table-row>
                            </xsl:for-each>
                        </fo:table-body>
                    </fo:table>

                    <fo:block margin-top="1cm"
                              text-align="right"
                              font-size="18pt"
                              font-weight="bold"
                              color="#111827">
                        GRAND TOTAL: <xsl:value-of select="$currencySymbol"/> <xsl:value-of select="$totalAmount"/>
                    </fo:block>

                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
</xsl:stylesheet>